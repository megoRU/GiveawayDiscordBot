package main.giveaway;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.controller.UpdateController;
import main.core.events.ReactionEvent;
import main.model.entity.ActiveGiveaways;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());

    //User LIST
    private final ConcurrentHashMap<String, String> listUsersHash;

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

    @Getter
    @Setter
    private volatile boolean isFinishGiveaway;

    @Getter
    @Setter
    private volatile boolean isLocked;

    @Getter
    @Setter
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
                    boolean isFinishGiveaway,
                    boolean isLocked,
                    GiveawayRepositoryService giveawayRepositoryService,
                    UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.listUsersHash = new ConcurrentHashMap<>(listUsersHash);
        this.giveawayData = giveawayData;
        this.isFinishGiveaway = isFinishGiveaway;
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
        activeGiveaways.setGuildId(guildId);
        activeGiveaways.setMessageId(message.getIdLong());
        activeGiveaways.setChannelId(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(this.giveawayData.countWinners);
        activeGiveaways.setTitle(this.giveawayData.title);
        activeGiveaways.setMinParticipants(this.giveawayData.minParticipants);

        if (this.giveawayData.roleId == null || this.giveawayData.roleId == 0) {
            activeGiveaways.setRoleId(null);
        } else {
            activeGiveaways.setRoleId(this.giveawayData.roleId);
        }
        activeGiveaways.setIsForSpecificRole(this.giveawayData.isForSpecificRole);
        activeGiveaways.setUrlImage(this.giveawayData.urlImage);
        activeGiveaways.setCreatedUserId(userIdLong);
        activeGiveaways.setDateEnd(this.giveawayData.endGiveawayDate == null ? null : this.giveawayData.endGiveawayDate);

        giveawayRepositoryService.saveGiveaway(activeGiveaways);
    }

    public synchronized void addUser(User... user) {
        GiveawayUserHandler giveawayUserHandler = new GiveawayUserHandler(giveawayRepositoryService);
        giveawayUserHandler.saveUser(this, user);
    }

    public synchronized void stopGiveaway(final int countWinner) {
        String logMessage = String.format(
                """
                        \n
                        stopGift method:
                                                
                        Guild ID: %s
                        ListUsersSize: %s
                        CountWinners: %s
                        """, guildId, listUsersHash.size(), countWinner);
        LOGGER.info(logMessage);

        GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
        giveawayEnds.stop(this, countWinner, updateController);
    }

    public boolean isUserContainsInGiveaway(String user) {
        return listUsersHash.containsKey(user);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public void addUserToList(String userId) {
        listUsersHash.put(userId, userId);
    }
}