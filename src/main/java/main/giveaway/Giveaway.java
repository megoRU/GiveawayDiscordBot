package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import main.config.Config;
import main.giveaway.impl.GiftHelper;
import main.giveaway.impl.URLS;
import main.giveaway.reactions.Reactions;
import main.giveaway.slash.SlashCommand;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<Future<?>> futureTasks = new ArrayList<>();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    //User LIST
    private final ConcurrentHashMap<String, String> listUsersHash;
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private final long userIdLong;

    //GiveawayData
    private long messageId;
    private int countWinners;
    private String title;
    private Timestamp endGiveawayDate;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;

    private final AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    private final ConcurrentLinkedQueue<Participants> participantsList = new ConcurrentLinkedQueue<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;

    public Giveaway(long guildId, long textChannelId, long userIdLong,
                    ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.listUsersHash = new ConcurrentHashMap<>();
        autoInsert();
    }

    public Giveaway(long guildId, long textChannelId, long userIdLong,
                    Map<String, String> listUsersHash,
                    ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository,
                    Long messageId,
                    int countWinners,
                    Long role,
                    boolean isOnlyForSpecificRole,
                    String urlImage,
                    String title,
                    Timestamp endGiveawayDate) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.listUsersHash = new ConcurrentHashMap<>(listUsersHash);

        this.messageId = messageId;
        this.countWinners = countWinners;
        this.roleId = role;
        this.isForSpecificRole = isOnlyForSpecificRole;
        this.urlImage = urlImage;
        this.title = title == null ? "Giveaway" : title;
        this.endGiveawayDate = endGiveawayDate;

        autoInsert();
    }

    public Timestamp updateTime(final String time) {
        if (time == null) return this.endGiveawayDate;
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(SlashCommand.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, SlashCommand.formatter);
        } else {
            long seconds = GiftHelper.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
        }

        long toEpochSecond = localDateTime.toEpochSecond(offset);
        this.endGiveawayDate = new Timestamp(toEpochSecond * 1000);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.cancelGiveawayTimer(guildId);

        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(this.guildId);
        Date date = new Date(this.endGiveawayDate.getTime());

        stopGiveawayByTimer.countDown();
        timer.schedule(stopGiveawayByTimer, date);
        instance.putGiveawayTimer(this.guildId, stopGiveawayByTimer, timer);
        return endGiveawayDate;
    }

    public void startGiveaway(GuildMessageChannel textChannel, String title, int countWinners,
                              String time, Long role, boolean isOnlyForSpecificRole,
                              String urlImage, boolean predefined) {
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


        this.title = title == null ? "Giveaway" : title;
        this.countWinners = countWinners;
        this.roleId = role;
        this.urlImage = urlImage;
        this.isForSpecificRole = isOnlyForSpecificRole;
        updateTime(time); //Обновляем время

        //Отправка сообщения
        if (predefined) {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayPattern(guildId).build())
                    .queue(this::updateCollections);
        } else {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayPattern(guildId).build())
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode(Reactions.TADA)).queue();
                        updateCollections(message);
                    });
        }
        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Message message) {
        this.messageId = message.getIdLong();

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);

        if (this.roleId == null || this.roleId == 0) {
            activeGiveaways.setRoleIdLong(null);
        } else {
            activeGiveaways.setRoleIdLong(this.roleId);
        }
        activeGiveaways.setIsForSpecificRole(this.isForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);
        activeGiveaways.setDateEndGiveaway(endGiveawayDate == null ? null : endGiveawayDate);

        activeGiveawayRepository.saveAndFlush(activeGiveaways);
        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {
        }
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

            //Add user to Collection
            Participants participants = new Participants();
            participants.setUserIdLong(user.getIdLong());
            participants.setNickName(user.getName());
            participants.setNickNameTag(user.getAsTag());
//            participants.setActiveGiveaways(activeGiveaways); //Can`t be null
            participantsList.add(participants);
        }
    }

    private synchronized void multiInsert() {
        try {
            if (count.get() > localCountUsers && GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                localCountUsers = count.get();
                if (!participantsList.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    Connection connection = DriverManager.getConnection(
                            Config.getDatabaseUrl(),
                            Config.getDatabaseUser(),
                            Config.getDatabasePass());
                    Statement statement = connection.createStatement();
                    int size = participantsList.size();
                    for (int i = 0; i < size; i++) {
                        Participants poll = participantsList.poll();
                        if (poll != null) {
                            stringBuilder
                                    .append(stringBuilder.length() == 0 ? "(" : ", (")
                                    .append("'").append(poll.getNickName()
                                            .replaceAll("'", "")
                                            .replaceAll("\"", "")
                                            .replaceAll("`", ""))
                                    .append("', ")
                                    .append(poll.getUserIdLong()).append(", ")
                                    .append(guildId).append(", ")
                                    .append("'").append(poll.getNickNameTag()
                                            .replaceAll("'", "")
                                            .replaceAll("\"", "")
                                            .replaceAll("`", ""))
                                    .append("')");
                        }
                    }

                    if (stringBuilder.length() != 0) {
                        String executeQuery = String.format("INSERT INTO participants (nick_name, user_long_id, guild_id, nick_name_tag) VALUES %s;", stringBuilder);
                        statement.execute(executeQuery);
                    }
                    statement.close();
                    connection.close();
                }
                if (participantsList.isEmpty()) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String format = String.format("Таблица: %s больше не существует, скорее всего Giveaway завершился!", guildId);
            LOGGER.info(format);
        }
    }

    /**
     * @throws Exception Throws an exception
     */
    private void getWinners(int countWinner) throws Exception {
        if (!participantsList.isEmpty()) {
            synchronized (this) {
                wait(10000L);
            }
        }
        List<Participants> participants = participantsRepository.getParticipantsByGuildIdLong(guildId); //TODO: Native use may be
        if (participants.isEmpty()) throw new Exception("participants is Empty");
        LOGGER.info("\nparticipants size: " + participants.size());

        StringBuilder stringBuilder = new StringBuilder();
        for (Participants participant : participants) {
            if (participant.getActiveGiveaways() != null) {
                stringBuilder
                        .append("getIdUserWhoCreateGiveaway ").append(participant.getActiveGiveaways().getIdUserWhoCreateGiveaway())
                        .append("getUserIdLong ").append(participant.getUserIdLong())
                        .append("getNickNameTag ").append(participant.getNickNameTag())
                        .append("getGiveawayId ").append(participant.getActiveGiveaways().getMessageIdLong())
                        .append("getGuildId ").append(participant.getActiveGiveaways().getGuildLongId())
                        .append("\n");
            }
        }
        System.out.println(stringBuilder);
        Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);
        LOGGER.info(winners.toString());
        String[] strings = api.setWinners(winners);
        for (String string : strings) {
            uniqueWinners.add("<@" + participants.get(Integer.parseInt(string)).getUserIdLong() + ">");
        }
    }

    public void stopGiveaway(final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winners: " + countWinner);
        GiftHelper giftHelper = new GiftHelper(activeGiveawayRepository);
        try {
            if (listUsersHash.size() < 2) {

                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", String.valueOf(guildId));
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", String.valueOf(guildId));

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);
                //Отправляет сообщение
                giftHelper.editMessage(notEnoughUsers, guildId, textChannelId);

                activeGiveawayRepository.deleteActiveGiveaways(guildId);
                //Удаляет данные из коллекций
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.clearingCollections(guildId);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //выбираем победителей
            getWinners(countWinner);
        } catch (Exception e) {
            if (futureTasks.isEmpty()) {
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", String.valueOf(guildId));
                String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", String.valueOf(guildId));
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setTitle(errorsWithApi);
                errors.setDescription(errorsDescriptions);
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                SenderMessage.sendMessage(errors.build(), guildId, textChannelId, buttons);
                StopGiveawayThread stopGiveawayThread = new StopGiveawayThread();
                Future<?> submit = executorService.submit(stopGiveawayThread);
                executorService.shutdown();
                futureTasks.add(submit);
                e.printStackTrace();
            }
            return;
        }

        EmbedBuilder urlEmbedded = new EmbedBuilder();
        urlEmbedded.setColor(Color.GREEN);
        String url = URLS.getDiscordUrlMessage(this.guildId, this.textChannelId, messageId);
        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        String winnersContent;
        if (uniqueWinners.size() == 1) {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations", String.valueOf(guildId)), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", String.valueOf(guildId)), url);
            urlEmbedded.setDescription(giftUrl);
            giftHelper.editMessage(GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId), this.guildId, textChannelId);
        } else {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations_many", String.valueOf(guildId)), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", String.valueOf(guildId)), url);
            urlEmbedded.setDescription(giftUrl);
            giftHelper.editMessage(GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId), this.guildId, textChannelId);
        }

        SenderMessage.sendMessage(urlEmbedded.build(), winnersContent, this.guildId, textChannelId);

        listUsersRepository.saveAllParticipantsToUserList(guildId);
        activeGiveawayRepository.deleteActiveGiveaways(guildId);

        //Удаляет данные из коллекций
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.clearingCollections(guildId);

        Stream<Boolean> booleanStream = futureTasks.stream().map(future -> future.cancel(true));
        futureTasks.clear();
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
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }, 2000, 5000);
    }

    public record GiveawayTimerStorage(StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
    }

    private class StopGiveawayThread implements Runnable {

        public void run() {
            try {
                while (true) {
                    if (!GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                        for (Future<?> futureTask : futureTasks) {
                            futureTask.cancel(true);
                            futureTasks.clear();
                        }
                        return;
                    }
                    stopGiveaway(countWinners);
                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                futureTasks.clear();
            }
        }
    }

    public boolean hasUserInGiveaway(String user) {
        return listUsersHash.containsKey(user);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getTextChannelId() {
        return textChannelId;
    }

    public long getUserIdLong() {
        return userIdLong;
    }

    public long getMessageId() {
        return messageId;
    }

    public int getCountWinners() {
        return countWinners;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getEndGiveawayDate() {
        return endGiveawayDate;
    }

    public Long getRoleId() {
        return roleId;
    }

    public boolean isForSpecificRole() {
        return isForSpecificRole;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public boolean isHasFutureTasks() {
        return futureTasks.isEmpty();
    }
}