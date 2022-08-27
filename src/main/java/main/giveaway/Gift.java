package main.giveaway;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.WinnersAndParticipants;
import api.megoru.ru.impl.MegoruAPIImpl;
import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.giveaway.buttons.ReactionsButton;
import main.giveaway.impl.GiftHelper;
import main.giveaway.impl.URLS;
import main.giveaway.reactions.Reactions;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Convector;
import main.model.entity.Notification;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static main.giveaway.impl.URLS.getDiscordUrlMessage;

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

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private final long userIdLong;

    private StringBuilder insertQuery = new StringBuilder();
    private AtomicInteger count = new AtomicInteger(0);
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
    }

    private void extracted(EmbedBuilder start, Guild guild, GuildMessageChannel channel,
                           String newTitle, int countWinners,
                           String time, Long role, boolean isOnlyForSpecificRole, String urlImage) {
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
            footer = "1 " + GiftHelper.setEndingWord(1, guildId);
        } else {
            footer = countWinners + " " + GiftHelper.setEndingWord(countWinners, guildId);
        }

        start.setFooter(footer);

        if (time != null) {
            String giftEndsAt = String.format(jsonParsers.getLocale("gift_ends_at", guild.getId()), footer);
            start.setFooter(giftEndsAt);
            ZoneOffset offset = ZoneOffset.UTC;
            LocalDateTime localDateTime;
            if (time.length() > 4) {
                localDateTime = LocalDateTime.parse(time, formatter);
                start.setTimestamp(localDateTime);
            } else {
                String minutes = GiftHelper.getMinutes(time);
                localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusMinutes(Long.parseLong(minutes));
                start.setTimestamp(localDateTime);
            }

            if (localDateTime.isBefore(Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime())) {
                throw new IllegalArgumentException(
                        "Time in the past " + localDateTime
                                + " Now: " + Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime());
            }

            start.appendDescription("\nEnds: <t:" + localDateTime.toEpochSecond(offset) + ":R> (<t:" + localDateTime.toEpochSecond(offset) + ":f>)");
            putTimestamp(localDateTime.toEpochSecond(offset));
        }

        start.appendDescription("\nHosted by: " + "<@" + this.userIdLong + ">");

        if (urlImage != null) {
            start.setImage(urlImage);
        }

    }

    public void startGift(@NotNull SlashCommandInteractionEvent event, Guild guild,
                          GuildMessageChannel textChannel, String newTitle, int countWinners,
                          String time, Long role, boolean isOnlyForSpecificRole,
                          String urlImage, Long idUserWhoCreateGiveaway) {

        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time, role, isOnlyForSpecificRole, urlImage);

        try {
            String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", guild.getId()), textChannel.getId());

            String message = sendSlashMessage + "\nIf you have lost an active Giveaway, please do not delete it and write to our support service" +
                    "\nWe will restore everything." +
                    "\nhttps://discord.gg/UrWG3R683d";

            event.reply(message)
                    .delay(120, TimeUnit.SECONDS)
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

    //Добавляет пользователя в StringBuilder
    public void addUserToPoll(final User user) {
        LOGGER.info("\n" + user.getName() + " " + user.getId());
        LOGGER.info("\n" + listUsersHash.containsKey(user.getId()));

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
                                        final String url = getDiscordUrlMessage(
                                                t.getGiveawayGuildId(),
                                                t.getActiveGiveaways().getChannelIdLong(),
                                                t.getActiveGiveaways().getMessageIdLong());

                                        final String giftRegistered = String.format(
                                                jsonParsers.getLocale("gift_registered", t.getGiveawayGuildId().toString()), url);


                                        final String giftVote = jsonParsers.getLocale("gift_vote", t.getGiveawayGuildId().toString());
                                        final String userIdLong = String.valueOf(t.getUserIdLong());
                                        final String giftRegisteredTitle = jsonParsers.getLocale("gift_registered_title", t.getGiveawayGuildId().toString());

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
}