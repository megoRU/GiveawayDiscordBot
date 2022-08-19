package main.giveaway;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.WinnersAndParticipants;
import api.megoru.ru.impl.MegoruAPIImpl;
import lombok.Getter;
import lombok.Setter;
import main.giveaway.impl.GiftHelper;
import main.giveaway.impl.URLS;
import main.giveaway.reactions.Reactions;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Convector;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ParticipantsRepository;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


@Getter
@Setter
public class Gift {

    private static final Logger LOGGER = Logger.getLogger(Gift.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPIImpl(System.getenv("BASE64_PASSWORD"));

    //User LIST
    private final Map<String, String> listUsersHash = new LinkedHashMap<>();
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //Time
    private Instant specificTime;
    private String times;
    private OffsetDateTime offsetTime;

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private final long userIdLong;

    private StringBuilder insertQuery = new StringBuilder();
    private AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    private volatile Set<Participants> participantsList = new HashSet<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    @Getter
    @Setter
    public class GiveawayData {

        private String channelId;
        private String messageId;
        private String countWinners;
        private String title;
        private Timestamp endGiveawayDate;
        private Long roleId;
        private boolean isForSpecificRole;
        private String urlImage;
        private String idUserWhoCreateGiveaway;

        public GiveawayData() {
        }

        public Gift getGift() {
            return Gift.this;
        }

        public boolean getIsForSpecificRole() {
            return isForSpecificRole;
        }

        public void setIsForSpecificRole(boolean is_for_specific_role) {
            isForSpecificRole = is_for_specific_role;
        }
    }

    public Gift(long guildId, long textChannelId, long userIdLong, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    private void extracted(EmbedBuilder start, Guild guild, GuildMessageChannel channel,
                           String newTitle, String countWinners,
                           String time, Long role, boolean isOnlyForSpecificRole, String urlImage) {
        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName() + " " + channel.getId()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time
                + "\nRole: " + role
                + "\nisOnlyForSpecificRole: " + isOnlyForSpecificRole
                + "\nurlImage: " + urlImage);
        //Instant для timestamp
        specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        String title = newTitle == null ? "Giveaway" : newTitle;

        start.setColor(Color.GREEN);
        start.setTitle(title);
        start.appendDescription(jsonParsers.getLocale("gift_reaction", guild.getId()));

        if (role != null) {
            if (isOnlyForSpecificRole) {
                channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                start.appendDescription(jsonParsers.getLocale("gift_OnlyFor", guild.getId()) + "<@&" + role + ">");
            } else {
                if (role == guildId) {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "@everyone").queue();
                } else {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                }
            }
        }

        String footer;
        if (countWinners == null) {
            footer = "1 " + GiftHelper.setEndingWord(1, guildId);
        } else {
            footer = countWinners + " " + GiftHelper.setEndingWord(Integer.parseInt(countWinners), guildId);
        }

        start.setFooter(footer);

        if (time != null) {
            start.setFooter(footer + " | " + jsonParsers.getLocale("gift_Ends_At", guild.getId()));
            ZoneOffset offset = ZoneOffset.UTC;
            LocalDateTime dateTime;
            if (time.length() > 4) {
                dateTime = LocalDateTime.parse(time, formatter);
                start.setTimestamp(dateTime);
            } else {
                times = GiftHelper.getMinutes(time);
                dateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusMinutes(Long.parseLong(times));
                start.setTimestamp(dateTime.plusMinutes(Long.parseLong(times)));
            }

            start.appendDescription("\nEnds: <t:" + dateTime.toEpochSecond(offset) + ":R> (<t:" + dateTime.toEpochSecond(offset) + ":f>)");
            putTimestamp(dateTime.toEpochSecond(offset));
        }

        start.appendDescription("\nHosted by: " + "<@" + userIdLong + ">");

        if (urlImage != null) {
            start.setImage(urlImage);
        }

    }

    public void startGift(@NotNull SlashCommandInteractionEvent event, Guild guild,
                          GuildMessageChannel textChannel, String newTitle, String countWinners,
                          String time, Long role, boolean isOnlyForSpecificRole,
                          String urlImage, Long idUserWhoCreateGiveaway) {

        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time, role, isOnlyForSpecificRole, urlImage);

        try {
            event.reply(jsonParsers.getLocale("send_slash_message", guild.getId()).replaceAll("\\{0}", textChannel.getId()))
                    .delay(7, TimeUnit.SECONDS)
                    .flatMap(InteractionHook::deleteOriginal)
                    .queue();
        } catch (Exception e) {
            e.printStackTrace();
        }

        textChannel.sendMessageEmbeds(start.build())
                .queue(message -> {
                    message.addReaction(Emoji.fromUnicode(Reactions.TADA)).queue();
                    updateCollections(countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle, idUserWhoCreateGiveaway);
                });

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(String countWinners, String time, Message message, Long role,
                                   Boolean isOnlyForSpecificRole, String urlImage, String title, Long idUserWhoCreateGiveaway) {

        GiveawayRegistry.getInstance().putMessageId(guildId, message.getId());
        GiveawayRegistry.getInstance().putChannelId(guildId, message.getChannel().getId());
        GiveawayRegistry.getInstance().putCountWinners(guildId, countWinners);
        GiveawayRegistry.getInstance().putRoleId(guildId, role);
        GiveawayRegistry.getInstance().putIsForSpecificRole(guildId, isOnlyForSpecificRole);
        GiveawayRegistry.getInstance().putUrlImage(guildId, urlImage);
        GiveawayRegistry.getInstance().putTitle(guildId, title == null ? "Giveaway" : title);
        GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(guildId, String.valueOf(idUserWhoCreateGiveaway));

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);
        activeGiveaways.setRoleIdLong(role);
        activeGiveaways.setIsForSpecificRole(isOnlyForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(idUserWhoCreateGiveaway);

        Timestamp endGiveawayDate = GiveawayRegistry.getInstance().getEndGiveawayDate(guildId);

        if (time != null && time.length() > 4) {
            activeGiveaways.setDateEndGiveaway(endGiveawayDate);
        } else {
            activeGiveaways.setDateEndGiveaway(time == null ? null : endGiveawayDate);
        }
        activeGiveawayRepository.saveAndFlush(activeGiveaways);
    }

    //Добавляет пользователя в StringBuilder
    public void addUserToPoll(final User user) {
        System.out.println(user.getName() + " " + user.getId());
        System.out.println(listUsersHash.containsKey(user.getId()));

        if (!listUsersHash.containsKey(user.getId())) {
            count.incrementAndGet();
            listUsersHash.put(user.getId(), user.getId());
            addUserToInsertQuery(user.getName(), user.getAsTag(), user.getIdLong(), guildId);
        }
    }

    private void executeMultiInsert(long guildIdLong) {
        try {
            if (count.get() > localCountUsers && GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                localCountUsers = count.get();
                if (participantsList != null && !participantsList.isEmpty()) {
                    //Сохраняем всех участников в temp коллекцию
                    Set<Participants> temp = new HashSet<>(participantsList);

                    participantsRepository.saveAllAndFlush(temp);

                    //Удаляем все элементы которые уже в БД
                    participantsList.removeAll(temp);
                }

                if (participantsList.isEmpty()) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            insertQuery = new StringBuilder();
            e.printStackTrace();
            System.out.println("Таблица: " + guildIdLong
                    + " больше не существует, скорее всего Giveaway завершился!\n"
                    + "Очищаем StringBuilder!");
        }
    }

    private void addUserToInsertQuery(final String nickName, final String nickNameTag, final long userIdLong, final long guildIdLong) {
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.getActiveGiveawaysByGuildIdLong(guildIdLong);
        Participants participants = new Participants();
        participants.setUserIdLong(userIdLong);
        participants.setNickName(nickName);
        participants.setNickNameTag(nickNameTag);
        participants.setActiveGiveaways(activeGiveaways);
        participantsList.add(participants);
    }

    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    public void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (GiveawayRegistry.getInstance().hasGift(guildId)) {
                        executeMultiInsert(guildId);
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

    /**
     * @throws Exception Throws an exception
     */
    private void getWinners(int countWinner) throws Exception {
        if (!participantsList.isEmpty()) {
            synchronized (this) {
                wait(10000L);
            }
        }

        List<api.megoru.ru.entity.Participants> participants =
                new Convector(participantsRepository.getParticipantsByGuildIdLong(guildId))
                        .getList();

        if (participants.isEmpty()) throw new Exception("participantsJSON is Empty");

        LOGGER.info("\nlistUsersHash size: " + listUsersHash.size());
        LOGGER.info("\nparticipantsJSON size: " + participants.size());

        for (int i = 0; i < participants.size(); i++) {
            System.out.println("getIdUserWhoCreateGiveaway " + participants.get(i).getIdUserWhoCreateGiveaway()
                    + " getUserIdLong " + participants.get(i).getUserIdLong()
                    + " getNickNameTag " + participants.get(i).getNickNameTag()
                    + " getGiveawayId " + participants.get(i).getGiveawayId());
        }

        try {
            Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);

            WinnersAndParticipants winnersAndParticipants = new WinnersAndParticipants();
            winnersAndParticipants.setUpdate(true);
            winnersAndParticipants.setWinners(winners);
            winnersAndParticipants.setUserList(participants);

            LOGGER.info(winners.toString());

            String[] strings = api.setWinners(winnersAndParticipants);

            List<String> temp = new LinkedList<>(listUsersHash.values());

            if (strings == null) throw new Exception("API not work, or connection refused");

            for (int i = 0; i < strings.length; i++) {
                uniqueWinners.add("<@" + temp.get(Integer.parseInt(strings[i])) + ">");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("API not work, or connection refused");
        }
    }

    public void stopGift(final long guildIdLong, final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        GiftHelper giftHelper = new GiftHelper(activeGiveawayRepository);
        try {
            if (listUsersHash.size() < 2) {
                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
                notEnoughUsers.setDescription(jsonParsers.getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
                //Отправляет сообщение
                giftHelper.editMessage(notEnoughUsers, guildIdLong, textChannelId);

                activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
                //Удаляет данные из коллекций
                clearingCollections();

                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //выбираем победителей
            getWinners(countWinner);
        } catch (Exception e) {
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.RED);
            errors.setTitle("Errors with API");
            errors.setDescription("Repeat later. Or write to us about it.");
            errors.appendDescription("\nYou have not completed the Giveaway");

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

            SenderMessage.sendMessage(errors.build(), guildId, textChannelId, buttons);
            return;
        }

        EmbedBuilder winners = new EmbedBuilder();
        winners.setColor(Color.GREEN);

        String messageId = GiveawayRegistry.getInstance().getMessageId(this.guildId);
        String url = URLS.getDiscordUrlMessage(String.valueOf(this.guildId), String.valueOf(this.textChannelId), messageId);

        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        if (uniqueWinners.size() == 1) {
            winners.setDescription(jsonParsers.getLocale("gift_congratulations",
                    String.valueOf(guildIdLong)).replaceAll("\\{0}", url)
                    + winnerArray);

            giftHelper.editMessage(
                    GiveawayEmbedUtils.embedBuilder(winnerArray, countWinner, guildIdLong),
                    this.guildId,
                    textChannelId);
        } else {
            winners.setDescription(jsonParsers.getLocale("gift_congratulations_many",
                    String.valueOf(guildIdLong)).replaceAll("\\{0}", url) + winnerArray);

            giftHelper.editMessage(
                    GiveawayEmbedUtils.embedBuilder(winnerArray, countWinner, guildIdLong),
                    this.guildId,
                    textChannelId);
        }

        SenderMessage.sendMessage(winners.build(), this.guildId, textChannelId);

        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);

        //Удаляет данные из коллекций
        clearingCollections();
    }

    private void putTimestamp(long localDateTime) {
        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(this.guildId);
        Timestamp timestamp = new Timestamp(localDateTime * 1000);
        Date date = new Date(timestamp.getTime());

        timer.schedule(stopGiveawayByTimer, date);

        GiveawayRegistry.getInstance().putEndGiveawayDate(this.guildId, timestamp);
        GiveawayRegistry.getInstance().putGiveawayTimer(this.guildId, timer);
    }

    private void clearingCollections() {
        try {
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(this.guildId);
            if (GiveawayRegistry.getInstance().getGiveawayTimer(this.guildId) != null) {
                GiveawayRegistry.getInstance().getGiveawayTimer(this.guildId).cancel();
            }
            GiveawayRegistry.getInstance().removeGiveawayTimer(this.guildId);
            setCount(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUserInList(String id) {
        return listUsersHash.containsKey(id);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public int getCount() {
        return count.intValue();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }
}