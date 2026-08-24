package main.giveaway;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStart;
import main.controller.UpdateController;
import main.model.entity.ActiveGiveaways;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Setter
public class Giveaway {

    private final static Logger LOGGER = LoggerFactory.getLogger(Giveaway.class.getName());

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private long userIdLong;

    //Giveaway properties previously in GiveawayData
    private final Set<Long> participantsList = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<ParticipantDTO>> queueConcurrentHashMap = new ConcurrentHashMap<>();
    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    private Instant endGiveawayDate;
    private int minParticipants = 1;

    //
    private volatile boolean isFinishGiveaway;
    private volatile boolean isRemoved;

    //REPO
    private final GiveawayRepositoryService giveawayRepositoryService;

    //CORE
    private final UpdateController updateController;

    public Giveaway(long guildId,
                    long textChannelId,
                    long userIdLong,
                    GiveawayRepositoryService giveawayRepositoryService,
                    UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.updateController = updateController;
        this.giveawayRepositoryService = giveawayRepositoryService;
    }

    public Giveaway(long guildId,
                    long textChannelId,
                    long userIdLong,
                    boolean isFinishGiveaway,
                    long messageId,
                    int countWinners,
                    Long roleId,
                    Boolean isForSpecificRole,
                    String urlImage,
                    String title,
                    Instant endGiveawayDate,
                    int minParticipants,
                    GiveawayRepositoryService giveawayRepositoryService,
                    UpdateController updateController) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.isFinishGiveaway = isFinishGiveaway;
        this.messageId = messageId;
        this.countWinners = countWinners;
        this.roleId = roleId;
        this.isForSpecificRole = Optional.ofNullable(isForSpecificRole).orElse(false);
        this.urlImage = urlImage;
        setTitle(title);
        this.minParticipants = minParticipants == 0 ? 1 : minParticipants;

        if (endGiveawayDate == null) {
            String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
            ZoneId zoneId = ZoneId.of(zonesIdByUser);
            endGiveawayDate = Instant.now().atZone(zoneId).plusDays(30).toInstant();
        }
        this.endGiveawayDate = endGiveawayDate;

        this.updateController = updateController;
        this.giveawayRepositoryService = giveawayRepositoryService;
    }

    public void setTitle(String title) {
        if (title == null) this.title = "Giveaway";
        else this.title = title;
    }

    public void setMinParticipants(int minParticipants) {
        if (minParticipants == 0) this.minParticipants = 1;
        else this.minParticipants = minParticipants;
    }

    public void addUserToQueue(User user) {
        String name = user.getName();
        long userIdLong = user.getIdLong();
        ParticipantDTO participantDTO = new ParticipantDTO(userIdLong, name);

        queueConcurrentHashMap.computeIfAbsent(messageId, _ -> new ConcurrentLinkedQueue<>()).add(participantDTO);
    }

    public int getParticipantSize() {
        return participantsList.size();
    }

    public void setParticipantsList(Set<Long> participantsMap) {
        participantsList.addAll(participantsMap);
    }

    public void updateTime(String time) throws ZoneRulesException {
        // Получаем тайм зону пользователя
        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
        ZoneId zoneId = ZoneId.of(zonesIdByUser);

        LocalDateTime localDateTime;

        if (time == null) {
            // если время не задано, ставим через 30 дней от текущего локального времени пользователя
            localDateTime = LocalDateTime.now(zoneId).plusDays(30);
        } else if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
            // если пришла дата в формате dd.MM.yyyy HH:mm
            localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        } else {
            // если пришли секунды
            long seconds = GiveawayUtils.getSeconds(time);
            localDateTime = LocalDateTime.now(zoneId).plusSeconds(seconds);
        }

        // Привязываем локальное время пользователя к его зоне
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

        // Переводим в Instant (UTC)
        Instant utcInstant = zonedDateTime.toInstant();

        // Сохраняем в MariaDB TIMESTAMP правильно, чтобы не было сдвига
        setEndGiveawayDate(utcInstant); //почему-то 2025-09-04T23:30:00
    }

    //TODO: Возможно добавлять в коллекцию тут
    public void startGiveaway(GuildMessageChannel textChannel, String title, int countWinners, String time, Long role,
                              Boolean isOnlyForSpecificRole, String urlImage, boolean predefined, int minParticipants) {
        setTitle(title);
        setCountWinners(countWinners);
        setRoleId(role);
        setUrlImage(urlImage);
        setForSpecificRole(isOnlyForSpecificRole != null && isOnlyForSpecificRole);
        setMinParticipants(minParticipants);
        setUserIdLong(userIdLong);
        updateTime(time); //Обновляем время

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(this);
        try {
            //Отправка сообщения
            Message message = textChannel.sendMessageEmbeds(embedBuilder.build()).submit().get();
            if (predefined) {
                updateOrCreateGiveaway(message);
            } else {
                message.addReaction(Emoji.fromUnicode(GiveawayUtils.TADA)).submit().get();
                updateOrCreateGiveaway(message);
            }

            long channelId = message.getChannel().getIdLong();
            long messageIdLong = message.getIdLong();

            //Записываем данные:
            LOGGER.info("GuildId: {} ChannelId: {} MessageId: {} Title: {} predefined: {} Winners: {} Time: {} Role: {} isOnlyForSpecificRole: {}",
                    guildId, channelId, messageIdLong, title, predefined, countWinners, time, role, isOnlyForSpecificRole);
        } catch (Exception e) {
            LOGGER.error("Error updating collections", e);
        }
    }

    private void updateOrCreateGiveaway(Message message) {
        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setMessageId(message.getIdLong());
        activeGiveaways.setGuildId(guildId);
        activeGiveaways.setChannelId(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(getCountWinners());
        activeGiveaways.setTitle(getTitle());
        activeGiveaways.setMinParticipants(getMinParticipants());

        if (getRoleId() == null || getRoleId() == 0) {
            activeGiveaways.setRoleId(null);
        } else {
            activeGiveaways.setRoleId(getRoleId());
        }
        activeGiveaways.setIsForSpecificRole(isForSpecificRole());
        activeGiveaways.setUrlImage(getUrlImage());
        activeGiveaways.setCreatedUserId(userIdLong);
        activeGiveaways.setEndGiveawayDate(getEndGiveawayDate());

        giveawayRepositoryService.saveGiveaway(activeGiveaways);
    }

    public synchronized void stopGiveaway(final int countWinner, ActiveGiveaways activeGiveaways) {
        LOGGER.info("stopGiveaway: GuildID: {}, ListUsersSize: {}, CountWinners: {}", guildId, getParticipantSize(), countWinner);
        GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
        giveawayEnds.stop(activeGiveaways, countWinner, updateController);
    }

    public synchronized void cancelGiveaway(ActiveGiveaways activeGiveaways) {
        LOGGER.info("cancelGiveaway: GuildID: {} GiveawayId: {}", guildId, getMessageId());
        GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
        giveawayEnds.cancel(activeGiveaways, updateController);
    }
}