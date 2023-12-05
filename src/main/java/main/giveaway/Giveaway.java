package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import lombok.Getter;
import lombok.NoArgsConstructor;
import main.controller.UpdateController;
import main.core.events.ReactionEvent;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import main.threads.StopGiveawayByTimer;
import main.threads.StopGiveawayThread;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    //User LIST
    private final ConcurrentHashMap<String, String> listUsersHash;
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //USER DATA
    @Getter
    private final long guildId, textChannelId, userIdLong;

    private ActiveGiveaways activeGiveaways;

    //GiveawayData
    private final GiveawayData giveawayData;

    private final AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    private final ConcurrentLinkedQueue<Participants> participantsList = new ConcurrentLinkedQueue<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;

    private final UpdateController updateController;

    @Getter
    @NoArgsConstructor
    public static class GiveawayData {
        private long messageId;
        private int countWinners;
        private Long roleId;
        private boolean isForSpecificRole;
        private String urlImage;
        private String title;
        private Timestamp endGiveawayDate;
        private int minParticipants = 2;

        public GiveawayData(long messageId,
                            int countWinners,
                            Long roleId,
                            boolean isForSpecificRole,
                            String urlImage,
                            String title,
                            Timestamp endGiveawayDate,
                            int minParticipants) {
            this.messageId = messageId;
            this.countWinners = countWinners;
            this.roleId = roleId;
            this.isForSpecificRole = isForSpecificRole;
            this.urlImage = urlImage;
            this.title = title;
            this.endGiveawayDate = endGiveawayDate;
            this.minParticipants = minParticipants;
        }
    }

    public Giveaway(long guildId, long textChannelId, long userIdLong,
                    ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository, UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.listUsersHash = new ConcurrentHashMap<>();
        this.giveawayData = new GiveawayData();
        this.updateController = updateController;
        autoInsert();
    }

    public Giveaway(long guildId, long textChannelId, long userIdLong,
                    Map<String, String> listUsersHash,
                    ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository,
                    GiveawayData giveawayData, UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.listUsersHash = new ConcurrentHashMap<>(listUsersHash);
        this.giveawayData = giveawayData;

        this.updateController = updateController;

        autoInsert();
    }

    public Timestamp updateTime(final String time) {
        if (time == null) return this.giveawayData.endGiveawayDate;
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        } else {
            long seconds = GiveawayUtils.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
        }

        long toEpochSecond = localDateTime.toEpochSecond(offset);
        this.giveawayData.endGiveawayDate = new Timestamp(toEpochSecond * 1000);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.cancelGiveawayTimer(guildId);

        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(this.guildId);
        Date date = new Date(this.giveawayData.endGiveawayDate.getTime());

        timer.schedule(stopGiveawayByTimer, date);
        instance.putGiveawayTimer(this.guildId, stopGiveawayByTimer, timer);
        return this.giveawayData.endGiveawayDate;
    }

    public void startGiveaway(GuildMessageChannel textChannel, String title, int countWinners,
                              String time, Long role, boolean isOnlyForSpecificRole,
                              String urlImage, boolean predefined, int minParticipants) {
        //Записываем данные:
        LOGGER.info("\nGuild id: " + guildId
                + "\nTextChannel: " + textChannel.getName() + " " + textChannel.getId()
                + "\nTitle: " + title
                + "\nCount winners: " + countWinners
                + "\nTime: " + time
                + "\nRole: " + role
                + "\nisOnlyForSpecificRole: " + isOnlyForSpecificRole
                + "\nurlImage: " + urlImage
                + "\npredefined: " + predefined);

        this.giveawayData.title = title == null ? "Giveaway" : title;
        this.giveawayData.countWinners = countWinners;
        this.giveawayData.roleId = role;
        this.giveawayData.urlImage = urlImage;
        this.giveawayData.isForSpecificRole = isOnlyForSpecificRole;
        this.giveawayData.minParticipants = minParticipants == 0 ? 2 : minParticipants;
        updateTime(time); //Обновляем время

        //Отправка сообщения
        if (predefined) {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayPattern(guildId).build())
                    .queue(this::updateCollections);
        } else {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayPattern(guildId).build())
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode(ReactionEvent.TADA)).queue();
                        updateCollections(message);
                    });
        }
        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Message message) {
        this.giveawayData.messageId = message.getIdLong();

        activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(this.giveawayData.countWinners);
        activeGiveaways.setGiveawayTitle(this.giveawayData.title);
        activeGiveaways.setMinParticipants(this.giveawayData.minParticipants);

        if (this.giveawayData.roleId == null || this.giveawayData.roleId == 0) activeGiveaways.setRoleIdLong(null);
        else activeGiveaways.setRoleIdLong(this.giveawayData.roleId);

        activeGiveaways.setIsForSpecificRole(this.giveawayData.isForSpecificRole);
        activeGiveaways.setUrlImage(this.giveawayData.urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);
        activeGiveaways.setDateEndGiveaway(this.giveawayData.endGiveawayDate == null ? null : this.giveawayData.endGiveawayDate);

        activeGiveawayRepository.save(activeGiveaways);
    }

    public void addUser(final User user) {
        LOGGER.info(String.format(
                """
                        \nНовый участник
                        Nick: %s
                        UserID: %s
                        Guild: %s
                        """,
                user.getName(),
                user.getId(),
                guildId));
        if (!listUsersHash.containsKey(user.getId())) {
            count.incrementAndGet();
            listUsersHash.put(user.getId(), user.getId());

            if (activeGiveaways == null) {
                Optional<ActiveGiveaways> optionalActiveGiveaways = activeGiveawayRepository.findById(guildId);
                if (optionalActiveGiveaways.isPresent()) activeGiveaways = optionalActiveGiveaways.get();
                else return;
            }

            //Add user to Collection
            Participants participants = new Participants();
            participants.setUserIdLong(user.getIdLong());
            participants.setNickName(user.getName());
            participants.setActiveGiveaways(activeGiveaways);
            participantsList.add(participants);
        }
    }

    private void multiInsert() {
        try {
            if (count.get() > localCountUsers && GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                localCountUsers = count.get();
                if (!participantsList.isEmpty()) {
                    int size = participantsList.size();
                    ArrayList<Participants> arrayList = new ArrayList<>(participantsList);
                    for (int i = 0; i < size; i++) {
                        Participants poll = participantsList.poll();
                        arrayList.add(poll);
                    }
                    participantsRepository.saveAll(arrayList);
                }
                if (participantsList.isEmpty()) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            String format = String.format("Таблица: %s больше не существует, скорее всего Giveaway завершился!", guildId);
            LOGGER.info(format);
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public void stopGiveaway(final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winners: " + countWinner);
        try {
            if (listUsersHash.size() < this.giveawayData.minParticipants) {

                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", String.valueOf(guildId));
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", String.valueOf(guildId));

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);
                //Отправляет сообщение

                updateController.setView(notEnoughUsers, guildId, textChannelId);

                activeGiveawayRepository.deleteById(guildId);
                //Удаляет данные из коллекций
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.clearingCollections(guildId);
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        try {
            //выбираем победителей
            if (!participantsList.isEmpty()) {
                synchronized (this) {
                    wait(10000L);
                }
            }
            List<Participants> participants = participantsRepository.findAllByActiveGiveaways_GuildLongId(guildId); //TODO: Native use may be
            if (participants.isEmpty()) throw new Exception("participants is Empty");

            LOGGER.info(String.format("Завершаем Giveaway: %s, Участников: %s", guildId, participants.size()));

            Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);
            String[] strings = api.setWinners(winners);
            for (String string : strings) {
                uniqueWinners.add("<@" + participants.get(Integer.parseInt(string)).getUserIdLong() + ">");
            }
        } catch (Exception e) {
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Future<?> future = instance.getFutureTasks(guildId);

            if (future == null) {
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", String.valueOf(guildId));
                String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", String.valueOf(guildId));
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setTitle(errorsWithApi);
                errors.setDescription(errorsDescriptions);
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                updateController.setView(errors.build(), guildId, textChannelId, buttons);

                //Создаем задачу
                StopGiveawayThread stopGiveawayThread = new StopGiveawayThread(this);
                Future<?> submit = executorService.submit(stopGiveawayThread);
                executorService.shutdown();
                instance.putFutureTasks(guildId, submit);
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
            return;
        }

        EmbedBuilder urlEmbedded = new EmbedBuilder();
        urlEmbedded.setColor(Color.GREEN);
        String url = GiveawayUtils.getDiscordUrlMessage(this.guildId, this.textChannelId, this.giveawayData.messageId);
        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        String winnersContent;
        if (uniqueWinners.size() == 1) {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations", String.valueOf(guildId)), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", String.valueOf(guildId)), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            updateController.setView(embedBuilder, guildId, textChannelId);
        } else {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations_many", String.valueOf(guildId)), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", String.valueOf(guildId)), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            updateController.setView(embedBuilder, guildId, textChannelId);
        }

        updateController.setView(urlEmbedded.build(), winnersContent, this.guildId, textChannelId);

        listUsersRepository.saveAllParticipantsToUserList(guildId);
        activeGiveawayRepository.deleteById(guildId);

        //Удаляет данные из коллекций
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.clearingCollections(guildId);

        Future<?> future = instance.getFutureTasks(guildId);
        if (future != null) {
            future.cancel(true);
            instance.removeFutureTasks(guildId);
        }
    }

    //TODO: Не завершается после завершения
    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    private void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                        multiInsert();
                    } else {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        }, 2000, 5000);
    }

    public record GiveawayTimerStorage(StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
    }

    public boolean isUsercontainsInGiveaway(String user) {
        return listUsersHash.containsKey(user);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }

    public long getMessageId() {
        return this.giveawayData.messageId;
    }

    public int getCountWinners() {
        return this.giveawayData.countWinners;
    }

    public String getTitle() {
        return this.giveawayData.title;
    }

    public Timestamp getEndGiveawayDate() {
        return this.giveawayData.endGiveawayDate;
    }

    public Long getRoleId() {
        return this.giveawayData.roleId;
    }

    public boolean isForSpecificRole() {
        return this.giveawayData.isForSpecificRole;
    }

    public String getUrlImage() {
        return this.giveawayData.urlImage;
    }

    public boolean isHasFutureTasks() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Future<?> futureTasks = instance.getFutureTasks(guildId);
        return futureTasks == null;
    }
}