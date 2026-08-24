package main.giveaway;

import main.config.BotStart;
import main.controller.UpdateController;
import main.model.entity.ActiveGiveaways;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

@Component
public class Giveaway {

    private static final Logger LOGGER = LoggerFactory.getLogger(Giveaway.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final UpdateController updateController;

    public Giveaway(GiveawayRepositoryService giveawayRepositoryService, UpdateController updateController) {
        this.giveawayRepositoryService = giveawayRepositoryService;
        this.updateController = updateController;
    }

    public ActiveGiveaways startGiveaway(GuildMessageChannel textChannel,
                                       Long userIdLong,
                                       Long guildId,
                                       String title,
                                       int countWinners,
                                       String time,
                                       Long role,
                                       Boolean isOnlyForSpecificRole,
                                       String urlImage,
                                       boolean isPredefined,
                                       int minParticipants) {

        Instant endGiveawayDate = calculateEndGiveawayDate(userIdLong, time);

        title = title == null ? "Giveaway" : title;
        minParticipants = minParticipants == 0 ? 1 : minParticipants;

        try {
            //Чисто по рофлу
            ActiveGiveaways activeGiveaways = new ActiveGiveaways();

            activeGiveaways.setGuildId(guildId);
            activeGiveaways.setCreatedUserId(userIdLong);
            activeGiveaways.setTitle(title);
            activeGiveaways.setCountWinners(countWinners);
            activeGiveaways.setEndGiveawayDate(endGiveawayDate);
            activeGiveaways.setRoleId(role);
            activeGiveaways.setIsForSpecificRole(isOnlyForSpecificRole);
            activeGiveaways.setUrlImage(urlImage);
            activeGiveaways.setMinParticipants(minParticipants);
            activeGiveaways.setChannelId(textChannel.getIdLong());
            activeGiveaways.setFinish(false);
            activeGiveaways.setPredefined(isPredefined);

            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(activeGiveaways);

            Message message = textChannel
                    .sendMessageEmbeds(embedBuilder.build())
                    .submit()
                    .get();

            if (!isPredefined) {
                message.addReaction(Emoji.fromUnicode(GiveawayUtils.TADA))
                        .submit()
                        .get();
            }

            ActiveGiveaways savedGiveaway = updateOrCreateGiveaway(
                    message,
                    guildId,
                    userIdLong,
                    title,
                    countWinners,
                    role,
                    isOnlyForSpecificRole,
                    urlImage,
                    endGiveawayDate,
                    minParticipants,
                    isPredefined
            );

            LOGGER.info(
                    "GuildId: {} ChannelId: {} MessageId: {} Title: {} predefined: {} Winners: {} Time: {} Role: {} isOnlyForSpecificRole: {}",
                    guildId,
                    message.getChannel().getIdLong(),
                    message.getIdLong(),
                    title,
                    isPredefined,
                    countWinners,
                    time,
                    role,
                    isOnlyForSpecificRole
            );

            return savedGiveaway;
        } catch (Exception e) {
            LOGGER.error("Error creating giveaway", e);
            return null;
        }
    }

    private ActiveGiveaways updateOrCreateGiveaway(Message message,
                                                 long guildId,
                                                 long userIdLong,
                                                 String title,
                                                 int countWinners,
                                                 Long roleId,
                                                 Boolean isForSpecificRole,
                                                 String urlImage,
                                                 Instant endGiveawayDate,
                                                 int minParticipants,
                                                 boolean isPredefined) {
        ActiveGiveaways activeGiveaways = new ActiveGiveaways();

        activeGiveaways.setMessageId(message.getIdLong());
        activeGiveaways.setGuildId(guildId);
        activeGiveaways.setChannelId(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setTitle(title);
        activeGiveaways.setMinParticipants(minParticipants);
        activeGiveaways.setRoleId(roleId == null || roleId == 0 ? null : roleId);
        activeGiveaways.setIsForSpecificRole(isForSpecificRole != null && isForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setCreatedUserId(userIdLong);
        activeGiveaways.setEndGiveawayDate(endGiveawayDate);
        activeGiveaways.setPredefined(isPredefined);

        giveawayRepositoryService.saveGiveaway(activeGiveaways);
        return activeGiveaways;
    }

    private Instant calculateEndGiveawayDate(long userIdLong, String time) throws ZoneRulesException {
        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
        ZoneId zoneId = ZoneId.of(zonesIdByUser);

        LocalDateTime localDateTime;

        if (time == null) {
            localDateTime = LocalDateTime.now(zoneId).plusDays(30);
        } else if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        } else {
            localDateTime = LocalDateTime.now(zoneId).plusSeconds(GiveawayUtils.getSeconds(time));
        }

        return localDateTime.atZone(zoneId).toInstant();
    }

    public void stopGiveaway(ActiveGiveaways activeGiveaways, int countWinner) {
        LOGGER.info("stopGiveaway: GuildID: {}, CountWinners: {}", activeGiveaways.getGuildId(), countWinner);

        GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
        giveawayEnds.stop(activeGiveaways, countWinner, updateController);
    }

    public void cancelGiveaway(ActiveGiveaways activeGiveaways) {
        LOGGER.info("cancelGiveaway: GuildID: {}, GiveawayId: {}", activeGiveaways.getGuildId(), activeGiveaways.getMessageId());

        GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
        giveawayEnds.cancel(activeGiveaways, updateController);
    }
}