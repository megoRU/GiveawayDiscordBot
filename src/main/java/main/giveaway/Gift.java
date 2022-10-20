package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.WinnersAndParticipants;
import api.megoru.ru.impl.MegoruAPI;
import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.giveaway.buttons.ReactionsButton;
import main.giveaway.impl.GiftHelper;
import main.giveaway.impl.URLS;
import main.giveaway.reactions.Reactions;
import main.giveaway.slash.SlashCommand;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Notification;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Setter
public class Gift {

    private static final Logger LOGGER = Logger.getLogger(Gift.class.getName());
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    //User LIST
    private final Map<String, String> listUsersHash;
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private final long userIdLong;

    private final AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    private volatile Set<Participants> participantsList = new LinkedHashSet<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    @Getter
    @Setter
    public class GiveawayData {

        private long channelId;
        private long messageId;
        private int countWinners;
        private String title;
        private Timestamp endGiveawayDate;
        private Long roleId;
        private boolean isForSpecificRole;
        private String urlImage;
        private long idUserWhoCreateGiveaway;

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
        this.listUsersHash = new LinkedHashMap<>();
        autoInsert();
    }

    public Gift(long guildId, long textChannelId, long userIdLong, Map<String, String> listUsersHash, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersHash = new LinkedHashMap<>(listUsersHash);
        autoInsert();
    }

    public void setTime(final EmbedBuilder start, final String time, final String footer) {
        String giftEndsAt = String.format(jsonParsers.getLocale("gift_ends_at", String.valueOf(guildId)), footer);
        start.setFooter(giftEndsAt);
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(SlashCommand.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, formatter);
            start.setTimestamp(localDateTime);
        } else {
            long seconds = GiftHelper.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
            start.setTimestamp(localDateTime);
        }

        if (localDateTime.isBefore(Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
            LocalDateTime localDateTimeThrow = Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
            String format = String.format("Time in the past %s Now %s", localDateTime, localDateTimeThrow);
            throw new IllegalArgumentException(format);
        }

        String endTimeFormat = String.format("\nEnds: <t:%s:R> (<t:%s:f>)", localDateTime.toEpochSecond(offset), localDateTime.toEpochSecond(offset));
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
                                   final String urlImage) {
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
        start.appendDescription(giftReaction);

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
            setTime(start, time, footer);
        }

        String hosted = String.format("\nHosted by: <@%s>", this.userIdLong);
        start.appendDescription(hosted);

        if (urlImage != null) {
            start.setImage(urlImage);
        }
        return start;
    }

    public void startGift(Guild guild,
                          GuildMessageChannel textChannel, String newTitle, int countWinners,
                          String time, Long role, boolean isOnlyForSpecificRole,
                          String urlImage, Long idUserWhoCreateGiveaway) {

        EmbedBuilder start = extracted(guild, textChannel, newTitle, countWinners, time, role, isOnlyForSpecificRole, urlImage);

        textChannel.sendMessageEmbeds(start.build())
                .queue(message -> {
                    message.addReaction(Emoji.fromUnicode(Reactions.TADA)).queue();
                    updateCollections(countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle, idUserWhoCreateGiveaway);
                });

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(int countWinners, String time, Message message, Long role,
                                   Boolean isOnlyForSpecificRole, String urlImage, String title, Long idUserWhoCreateGiveaway) {

        GiveawayRegistry.getInstance().putMessageId(guildId, message.getIdLong());
        GiveawayRegistry.getInstance().putChannelId(guildId, message.getChannel().getIdLong());
        GiveawayRegistry.getInstance().putCountWinners(guildId, countWinners);
        GiveawayRegistry.getInstance().putRoleId(guildId, role);
        GiveawayRegistry.getInstance().putIsForSpecificRole(guildId, isOnlyForSpecificRole);
        GiveawayRegistry.getInstance().putUrlImage(guildId, urlImage);
        GiveawayRegistry.getInstance().putTitle(guildId, title == null ? "Giveaway" : title);
        GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(guildId, idUserWhoCreateGiveaway);

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

    public synchronized void addUserToPoll(final User user) {
        LOGGER.info(String.format(
                """
                        \nНовый участник
                        Nick: %s
                        UserID: %s
                        Guild: %s""",
                user.getName(),
                user.getId(),
                guildId));
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
                    Set<Participants> temp = new LinkedHashSet<>(participantsList);

                    participantsRepository.saveAllAndFlush(temp);

                    String buttonNotification = jsonParsers.getLocale("button_notification", String.valueOf(this.guildId));
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(Button.danger(ReactionsButton.DISABLE_NOTIFICATIONS, buttonNotification));

                    for (int i = 0; i < temp.size(); i++) {
                        temp.forEach(t -> {
                                    Notification.NotificationStatus notificationStatus = BotStartConfig
                                            .getMapNotifications()
                                            .get(String.valueOf(t.getUserIdLong()));

                                    if (notificationStatus == null) {
                                        notificationStatus = Notification.NotificationStatus.ACCEPT;
                                    }

                                    if (notificationStatus.equals(Notification.NotificationStatus.ACCEPT)) {
                                        final String url = URLS.getDiscordUrlMessage(
                                                guildIdLong,
                                                textChannelId,
                                                t.getActiveGiveaways().getMessageIdLong());

                                        final String giftRegistered = String.format(jsonParsers.getLocale("gift_registered", String.valueOf(t.getGuildIdLong())), url);

                                        final String giftVote = jsonParsers.getLocale("gift_vote", String.valueOf(t.getGuildIdLong()));
                                        final String userIdLong = String.valueOf(t.getUserIdLong());
                                        final String giftRegisteredTitle = jsonParsers.getLocale("gift_registered_title", String.valueOf(t.getGuildIdLong()));

                                        EmbedBuilder embedBuilder = new EmbedBuilder();
                                        embedBuilder.setColor(Color.GREEN);
                                        embedBuilder.setAuthor(
                                                giftRegisteredTitle,
                                                null,
                                                BotStartConfig.getJda().getSelfUser().getAvatarUrl());
                                        embedBuilder.setDescription(giftRegistered);
                                        embedBuilder.appendDescription(giftVote);

                                        SenderMessage.sendPrivateMessageWithButtons(
                                                BotStartConfig.getJda(),
                                                userIdLong,
                                                embedBuilder.build(), buttons);
                                    }
                                }
                        );
                    }

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
        participants.setIdUserWhoCreateGiveaway(activeGiveaways.getIdUserWhoCreateGiveaway().toString());
        participants.setGuildIdLong(guildIdLong);
        participants.setGiveawayIdLong(String.valueOf(activeGiveaways.getGuildLongId() + activeGiveaways.getMessageIdLong()));
        participantsList.add(participants);
    }

    //TODO: Не завершается после заверешения
    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    private void autoInsert() {
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

        List<Participants> participants = participantsRepository.getParticipantsByGuildIdLong(guildId);

        if (participants.isEmpty()) throw new Exception("participantsJSON is Empty");

        LOGGER.info("\nlistUsersHash size: " + listUsersHash.size());
        LOGGER.info("\nparticipantsJSON size: " + participants.size());

        for (Participants participant : participants) {
            System.out.println("getIdUserWhoCreateGiveaway " + participant.getIdUserWhoCreateGiveaway()
                    + " getUserIdLong " + participant.getUserIdLong()
                    + " getNickNameTag " + participant.getNickNameTag()
                    + " getGiveawayId " + participant.getGiveawayIdLong()
                    + " getGuildId " + participant.getGuildIdLong()
            );
        }
        long messageId = GiveawayRegistry.getInstance().getMessageId(guildId);
        long giveawayIdLong = guildId + messageId;

        Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);

        WinnersAndParticipants winnersAndParticipants = new WinnersAndParticipants();
        winnersAndParticipants.setGiveawayID(String.valueOf(giveawayIdLong));
        winnersAndParticipants.setWinners(winners);
        winnersAndParticipants.setUserList(participants);

        LOGGER.info(winners.toString());

        List<String> temp = new LinkedList<>(listUsersHash.values());

        String[] strings = api.setWinners(winnersAndParticipants);

        for (String string : strings) {
            uniqueWinners.add("<@" + temp.get(Integer.parseInt(string)) + ">");
        }
    }

    public void stopGift(final long guildIdLong, final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        GiftHelper giftHelper = new GiftHelper(activeGiveawayRepository);
        try {
            if (listUsersHash.size() < 2) {

                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", String.valueOf(guildIdLong));
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", String.valueOf(guildIdLong));

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);
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
            e.printStackTrace();
            return;
        }

        EmbedBuilder winners = new EmbedBuilder();
        winners.setColor(Color.GREEN);

        long messageId = GiveawayRegistry.getInstance().getMessageId(this.guildId);
        String url = URLS.getDiscordUrlMessage(this.guildId, this.textChannelId, messageId);

        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        if (uniqueWinners.size() == 1) {
            String giftCongratulations = String.format(jsonParsers.getLocale("gift_congratulations", String.valueOf(guildIdLong)), url, winnerArray);
            winners.setDescription(giftCongratulations);

            giftHelper.editMessage(
                    GiveawayEmbedUtils.embedBuilder(winnerArray, countWinner, guildIdLong),
                    this.guildId,
                    textChannelId);
        } else {
            String giftCongratulationsMany = String.format(jsonParsers.getLocale("gift_congratulations_many", String.valueOf(guildIdLong)), url, winnerArray);
            winners.setDescription(giftCongratulationsMany);

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

    @Getter
    public static class GiveawayTimerStorage {

        private final StopGiveawayByTimer stopGiveawayByTimer;
        private final Timer timer;

        public GiveawayTimerStorage(StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
            this.stopGiveawayByTimer = stopGiveawayByTimer;
            this.timer = timer;
        }
    }

    public void putTimestamp(long localDateTime) {
        GiveawayRegistry.getInstance().cancelGiveawayTimer(guildId);

        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(this.guildId);
        Timestamp timestamp = new Timestamp(localDateTime * 1000);
        Date date = new Date(timestamp.getTime());

        stopGiveawayByTimer.countDown();
        timer.schedule(stopGiveawayByTimer, date);

        GiveawayRegistry.getInstance().putEndGiveawayDate(this.guildId, timestamp);
        GiveawayRegistry.getInstance().putGiveawayTimer(this.guildId, stopGiveawayByTimer, timer);
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

    public boolean hasUserInList(String id) {
        return listUsersHash.containsKey(id);
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
}