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
import net.dv8tion.jda.api.entities.Guild;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final JSONParsers jsonParsers = new JSONParsers();

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

    public void setTime(final EmbedBuilder start, final String time) {
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(SlashCommand.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, formatter);
        } else {
            long seconds = GiftHelper.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
        }

        if (localDateTime.isBefore(Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
            LocalDateTime localDateTimeThrow = Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
            String wrongDate = jsonParsers.getLocale("wrong_date", String.valueOf(guildId));

            String format = String.format("Time in the past: `%s`\nNow:`%s`",
                    localDateTime.toString().replace("T", " "),
                    localDateTimeThrow.toString().substring(0, 16).replace("T", " "));

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.RED);
            embedBuilder.setTitle(wrongDate);
            embedBuilder.setDescription(format);

            SenderMessage.sendMessage(embedBuilder.build(), guildId, textChannelId);
            throw new IllegalArgumentException(format);
        }

        String endTimeFormat =
                String.format(jsonParsers.getLocale("gift_ends_giveaway", String.valueOf(guildId)),
                        localDateTime.toEpochSecond(offset),
                        localDateTime.toEpochSecond(offset));
        start.appendDescription(endTimeFormat);

        putTimestamp(localDateTime.toEpochSecond(offset));
    }

    private EmbedBuilder extracted(final Guild guild,
                                   final GuildMessageChannel channel,
                                   final String newTitle,
                                   final int countWinners,
                                   final String time,
                                   final Long role,
                                   final boolean isOnlyForSpecificRole,
                                   final String urlImage,
                                   final boolean predefined) {
        EmbedBuilder start = new EmbedBuilder();

        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName() + " " + channel.getId()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time
                + "\nRole: " + role
                + "\nisOnlyForSpecificRole: " + isOnlyForSpecificRole
                + "\nurlImage: " + urlImage);

        String title = newTitle == null ? "Giveaway" : newTitle;
        String giftReaction = jsonParsers.getLocale("gift_reaction", guild.getId());

        start.setColor(Color.GREEN);
        start.setTitle(title);
        if (!predefined) {
            start.appendDescription(giftReaction);
        }

        if (role != null) {
            String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()), role);
            if (isOnlyForSpecificRole) {
                channel.sendMessage(giftNotificationForThisRole).queue();
                String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guild.getId()), role);
                start.appendDescription(giftOnlyFor);
            } else {
                if (role == guildId) {
                    String notificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guild.getId()), "@everyone");
                    channel.sendMessage(notificationForThisRole).queue();
                } else {
                    channel.sendMessage(giftNotificationForThisRole).queue();
                }
            }
        }

        String footer;
        if (countWinners == 1) {
            footer = String.format("1 %s", GiftHelper.setEndingWord(1, guildId));
        } else {
            footer = String.format("%s %s", countWinners, GiftHelper.setEndingWord(countWinners, guildId));
        }

        start.setFooter(footer);

        if (time != null) {
            setTime(start, time);
        }

        String hosted = String.format("\nHosted by: <@%s>", this.userIdLong);
        start.appendDescription(hosted);

        if (urlImage != null) {
            start.setImage(urlImage);
        }
        return start;
    }

    public void startGiveaway(Guild guild,
                              GuildMessageChannel textChannel, String newTitle, int countWinners,
                              String time, Long role, boolean isOnlyForSpecificRole,
                              String urlImage, boolean predefined) {

        EmbedBuilder start = extracted(guild, textChannel, newTitle, countWinners, time, role, isOnlyForSpecificRole, urlImage, predefined);

        if (predefined) {
            textChannel.sendMessageEmbeds(start.build())
                    .queue(message -> updateCollections(countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle));
        } else {
            textChannel.sendMessageEmbeds(start.build())
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode(Reactions.TADA)).queue();
                        updateCollections(countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle);
                    });
        }

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(int countWinners, String time, Message message, Long role,
                                   Boolean isOnlyForSpecificRole, String urlImage, String title) {
        this.messageId = message.getIdLong();
        this.countWinners = countWinners;
        this.roleId = role;
        this.isForSpecificRole = isOnlyForSpecificRole;
        this.urlImage = urlImage;
        this.title = title == null ? "Giveaway" : title;

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);
        activeGiveaways.setRoleIdLong(role);
        activeGiveaways.setIsForSpecificRole(isOnlyForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);

        if (time != null && time.length() > 4) {
            activeGiveaways.setDateEndGiveaway(endGiveawayDate);
        } else {
            activeGiveaways.setDateEndGiveaway(time == null ? null : endGiveawayDate);
        }
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
                                    .append("'").append(poll.getNickName()).append("', ")
                                    .append(poll.getUserIdLong()).append(", ")
                                    .append(guildId).append(", ")
                                    .append("'").append(poll.getNickNameTag()).append("')");
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
        LOGGER.info("\nlistUsersHash size: " + listUsersHash.size());
        LOGGER.info("\nparticipantsJSON size: " + participants.size());

        if (listUsersHash.size() != participants.size()) {
            throw new Exception(
                    "listUsersHash.size(): " + listUsersHash.size() +
                            " != participants.size(): " + participants.size());
        }

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
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
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
            e.printStackTrace();
            return;
        }

        EmbedBuilder winners = new EmbedBuilder();
        winners.setColor(Color.GREEN);

        String url = URLS.getDiscordUrlMessage(this.guildId, this.textChannelId, messageId);

        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        if (uniqueWinners.size() == 1) {
            String giftCongratulations = String.format(jsonParsers.getLocale("gift_congratulations", String.valueOf(guildId)), url, winnerArray);
            winners.setDescription(giftCongratulations);

            giftHelper.editMessage(
                    GiveawayEmbedUtils.embedBuilder(winnerArray, countWinner, guildId),
                    this.guildId,
                    textChannelId);
        } else {
            String giftCongratulationsMany = String.format(jsonParsers.getLocale("gift_congratulations_many", String.valueOf(guildId)), url, winnerArray);
            winners.setDescription(giftCongratulationsMany);

            giftHelper.editMessage(
                    GiveawayEmbedUtils.embedBuilder(winnerArray, countWinner, guildId),
                    this.guildId,
                    textChannelId);
        }

        SenderMessage.sendMessage(winners.build(), this.guildId, textChannelId);

        listUsersRepository.saveAllParticipantsToUserList(guildId);
        activeGiveawayRepository.deleteActiveGiveaways(guildId);

        //Удаляет данные из коллекций
        clearingCollections();
    }

    //TODO: Не завершается после заверешения
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

    public void putTimestamp(long localDateTime) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.cancelGiveawayTimer(guildId);

        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(this.guildId);
        Timestamp timestamp = new Timestamp(localDateTime * 1000);
        Date date = new Date(timestamp.getTime());

        stopGiveawayByTimer.countDown();
        timer.schedule(stopGiveawayByTimer, date);

        endGiveawayDate = timestamp;
        instance.putGiveawayTimer(this.guildId, stopGiveawayByTimer, timer);
    }

    private void clearingCollections() {
        try {
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(this.guildId);
            GiveawayTimerStorage giveawayTimer = GiveawayRegistry.getInstance().getGiveawayTimer(this.guildId);

            if (giveawayTimer != null) {
                giveawayTimer.stopGiveawayByTimer.cancel();
                giveawayTimer.stopGiveawayByTimer.countDown();
            }
            GiveawayRegistry.getInstance().removeGiveawayTimer(this.guildId);
            setCount(0);
        } catch (Exception e) {
            e.printStackTrace();
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
}