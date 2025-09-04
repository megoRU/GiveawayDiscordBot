package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Collection;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ScheduleStartService {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleStartService.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final SchedulingRepository schedulingRepository;
    private final static GiveawayRegistry instance = GiveawayRegistry.getInstance();

    public void scheduleStart(UpdateController updateController, JDA jda) {
        Collection<Scheduling> scheduledGiveaways = instance.getScheduledGiveaways();

        for (Scheduling scheduling : scheduledGiveaways) {
            Long createdUserId = scheduling.getCreatedUserId();
            String zonesIdByUser = BotStart.getZonesIdByUser(createdUserId);

            ZoneId offset = ZoneId.of(zonesIdByUser);
            ZonedDateTime odt = Instant.now().atZone(offset);
            Instant instant = odt.toInstant();

            Instant dateCreateGiveaway = scheduling.getDateCreateGiveaway();

            //TODO: проверить, надо еще перевести дату окончания в atZone
            if (instant.isAfter(dateCreateGiveaway)) {
                try {
                    Long channelIdLong = scheduling.getChannelId();
                    Guild guildById = jda.getGuildById(scheduling.getGuildId());

                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelIdLong);
                        if (textChannelById != null) {
                            Long role = scheduling.getRoleId();
                            Boolean isOnlyForSpecificRole = scheduling.getIsForSpecificRole();
                            Long guildIdLong = scheduling.getGuildId();
                            Long guildId = scheduling.getGuildId();
                            String idSalt = scheduling.getIdSalt();

                            try {
                                Giveaway giveaway = new Giveaway(
                                        scheduling.getGuildId(),
                                        textChannelById.getIdLong(),
                                        scheduling.getCreatedUserId(),
                                        giveawayRepositoryService,
                                        updateController);

                                //TODO: возможно нужно сначала atZone и не делать toInstant
                                Instant endInstant = scheduling.getDateEndGiveaway();
                                LocalDateTime dateEndGiveaway = endInstant.atZone(offset).toLocalDateTime();
                                String formattedDate = dateEndGiveaway.format(GiveawayUtils.FORMATTER);

                                if (role != null && isOnlyForSpecificRole) {
                                    String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                                    if (Objects.equals(role, guildIdLong)) {
                                        giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                                        textChannelById.sendMessage(giftNotificationForThisRole).queue();
                                    } else {
                                        textChannelById.sendMessage(giftNotificationForThisRole).queue();
                                    }
                                }

                                giveaway.startGiveaway(
                                        textChannelById,
                                        scheduling.getTitle(),
                                        scheduling.getCountWinners(),
                                        formattedDate,
                                        scheduling.getRoleId(),
                                        scheduling.getIsForSpecificRole(),
                                        scheduling.getUrlImage(),
                                        false,
                                        scheduling.getMinParticipants());

                                long messageId = giveaway.getGiveawayData().getMessageId();

                                instance.removeScheduling(idSalt); //Чтобы не моросил
                                instance.putGift(messageId, giveaway);

                                schedulingRepository.deleteByIdSalt(idSalt);
                            } catch (ZoneRulesException z) {
                                LOGGER.error(z.getMessage(), z);

                                String startWithBrokenZone = jsonParsers.getLocale("start_with_broken_zone", guildId);

                                EmbedBuilder errors = new EmbedBuilder();
                                errors.setColor(Color.GREEN);
                                errors.setDescription(startWithBrokenZone);

                                textChannelById.sendMessageEmbeds(errors.build()).queue();
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}