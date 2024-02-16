package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.controller.UpdateController;
import main.core.events.ReactionEvent;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    //User LIST
    private final ConcurrentHashMap<String, String> listUsersHash;
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //USER DATA
    @Getter
    private final long guildId;
    @Getter
    private final long textChannelId;
    @Getter
    private final long userIdLong;

    //GiveawayData
    @Getter
    private final GiveawayData giveawayData;

    private final UpdateController updateController;

    private final AtomicInteger count = new AtomicInteger(0);

    @Getter
    private volatile boolean finishGiveaway;

    @Getter
    @Setter
    private volatile boolean isLocked;

    @Getter
    private volatile boolean isRemoved;

    //REPO
    private final GiveawayRepositoryService giveawayRepositoryService;

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

    public Giveaway(long guildId,
                    long textChannelId,
                    long userIdLong,
                    GiveawayRepositoryService giveawayRepositoryService,
                    UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.listUsersHash = new ConcurrentHashMap<>();
        this.giveawayData = new GiveawayData();
        this.updateController = updateController;
        this.giveawayRepositoryService = giveawayRepositoryService;
    }

    public Giveaway(long guildId, long textChannelId, long userIdLong,
                    Map<String, String> listUsersHash,
                    GiveawayData giveawayData,
                    boolean finishGiveaway,
                    boolean isLocked,
                    GiveawayRepositoryService giveawayRepositoryService,
                    UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.listUsersHash = new ConcurrentHashMap<>(listUsersHash);
        this.giveawayData = giveawayData;
        this.finishGiveaway = finishGiveaway;
        this.isLocked = isLocked;
        this.updateController = updateController;
        this.giveawayRepositoryService = giveawayRepositoryService;
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
    }

    private void updateCollections(Message message) {
        this.giveawayData.messageId = message.getIdLong();

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(this.giveawayData.countWinners);
        activeGiveaways.setGiveawayTitle(this.giveawayData.title);
        activeGiveaways.setMinParticipants(this.giveawayData.minParticipants);

        if (this.giveawayData.roleId == null || this.giveawayData.roleId == 0) {
            activeGiveaways.setRoleIdLong(null);
        } else {
            activeGiveaways.setRoleIdLong(this.giveawayData.roleId);
        }
        activeGiveaways.setIsForSpecificRole(this.giveawayData.isForSpecificRole);
        activeGiveaways.setUrlImage(this.giveawayData.urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);
        activeGiveaways.setDateEndGiveaway(this.giveawayData.endGiveawayDate == null ? null : this.giveawayData.endGiveawayDate);

        giveawayRepositoryService.saveGiveaway(activeGiveaways);
    }

    public void addUser(User... user) {
        if (user.length == 1) {
            User userLocal = user[0];
            if (!listUsersHash.containsKey(userLocal.getId())) {
                LOGGER.info(String.format(
                        """
                                \nНовый участник
                                Nick: %s
                                UserID: %s
                                Guild: %s
                                """,
                        userLocal.getName(),
                        userLocal.getId(),
                        guildId));
                count.incrementAndGet();

                listUsersHash.put(userLocal.getId(), userLocal.getId());

                if (!isRemoved) {
                    ActiveGiveaways giveaway = giveawayRepositoryService.getGiveaway(guildId);

                    Participants participants = new Participants();
                    participants.setUserIdLong(userLocal.getIdLong());
                    participants.setNickName(userLocal.getName());
                    participants.setActiveGiveaways(giveaway);

                    giveawayRepositoryService.saveParticipants(participants);
                }
            }
        } else {
            List<User> userList = Arrays.stream(user)
                    .filter(users -> listUsersHash.containsKey(users.getId()))
                    .toList();

            if (!isRemoved) {
                ActiveGiveaways giveaway = giveawayRepositoryService.getGiveaway(guildId);

                Participants[] participantsList = new Participants[userList.size()];
                for (int i = 0; i < userList.size(); i++) {
                    User users = userList.get(i);

                    Participants participants = new Participants();
                    participants.setUserIdLong(users.getIdLong());
                    participants.setNickName(users.getName());
                    participants.setActiveGiveaways(giveaway);

                    participantsList[i] = participants;
                }
                giveawayRepositoryService.saveParticipants(participantsList);
            }
        }
    }

    public void stopGiveaway(final int countWinner) {
        String logMessage = String.format(
                """
                        \n
                        stopGift method:
                                                
                        Guild ID: %s
                        ListUsersSize: %s
                        CountWinners: %s
                        """, guildId, listUsersHash.size(), countWinner);
        LOGGER.info(logMessage);

        Color userColor = GiveawayUtils.getUserColor(guildId);
        try {
            if (listUsersHash.size() < this.giveawayData.minParticipants) {
                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(userColor);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);
                //Отправляет сообщение

                updateController.setView(notEnoughUsers, guildId, textChannelId);

                giveawayRepositoryService.deleteGiveaway(guildId);
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.removeGuildFromGiveaway(guildId);
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        try {
            List<Participants> participants = giveawayRepositoryService.findAllParticipants(guildId); //TODO: Native use may be
            if (participants.isEmpty()) throw new Exception("participants is Empty");

            LOGGER.info(String.format("Завершаем Giveaway: %s, Участников: %s", guildId, participants.size()));

            Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);
            List<String> strings = api.getWinners(winners);
            for (String string : strings) {
                uniqueWinners.add("<@" + participants.get(Integer.parseInt(string)).getUserIdLong() + ">");
            }
        } catch (Exception e) {
            if (!finishGiveaway) {
                finishGiveaway = true;
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", guildId);
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setTitle(errorsWithApi);
                errors.setDescription(errorsDescriptions);
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                updateController.setView(errors.build(), guildId, textChannelId, buttons);

                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
            return;
        }

        EmbedBuilder urlEmbedded = new EmbedBuilder();
        urlEmbedded.setColor(userColor);
        String url = GiveawayUtils.getDiscordUrlMessage(this.guildId, this.textChannelId, this.giveawayData.messageId);
        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        String winnersContent;
        if (uniqueWinners.size() == 1) {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations", guildId), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            updateController.setView(embedBuilder, guildId, textChannelId);
        } else {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations_many", guildId), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            updateController.setView(embedBuilder, guildId, textChannelId);
        }

        updateController.setView(urlEmbedded.build(), winnersContent, this.guildId, textChannelId);

        isRemoved = true;
        //Удаляет данные из коллекций
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeGuildFromGiveaway(guildId);

        giveawayRepositoryService.backupAllParticipants(guildId);
        giveawayRepositoryService.deleteGiveaway(guildId);
    }

    public boolean isUsercontainsInGiveaway(String user) {
        return listUsersHash.containsKey(user);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public void setCount(int count) {
        this.count.set(count);
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
}