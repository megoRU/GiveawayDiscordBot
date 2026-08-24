package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
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
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ScheduleStartService {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleStartService.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final SchedulingRepository schedulingRepository;

    @Transactional
    public void scheduleStart(String idSalt, UpdateController updateController, JDA jda) {
        Scheduling scheduling = schedulingRepository.findByIdSalt(idSalt);
        if (scheduling == null) return;

        Long createdUserId = scheduling.getCreatedUserId();
        String zonesIdByUser = BotStart.getZonesIdByUser(createdUserId);
        ZoneId offset = ZoneId.of(zonesIdByUser);

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

                    try {
                        Giveaway giveaway = new Giveaway(giveawayRepositoryService, updateController);

                        Instant endInstant = scheduling.getDateEndGiveaway();
                        LocalDateTime dateEndGiveaway = endInstant.atZone(offset).toLocalDateTime();
                        String formattedDate = dateEndGiveaway.format(GiveawayUtils.FORMATTER);

                        if (role != null && isOnlyForSpecificRole) {
                            String giftNotificationForThisRole =
                                    String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);

                            if (Objects.equals(role, guildIdLong)) {
                                giftNotificationForThisRole =
                                        String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                                textChannelById.sendMessage(giftNotificationForThisRole).queue();
                            } else {
                                textChannelById.sendMessage(giftNotificationForThisRole).queue();
                            }
                        }

                        giveaway.startGiveaway(
                                textChannelById,
                                scheduling.getCreatedUserId(),
                                guildId,
                                scheduling.getTitle(),
                                scheduling.getCountWinners(),
                                formattedDate,
                                scheduling.getRoleId(),
                                scheduling.getIsForSpecificRole(),
                                scheduling.getUrlImage(),
                                false,
                                scheduling.getMinParticipants()
                        );

                        schedulingRepository.deleteByIdSalt(idSalt);
                    } catch (ZoneRulesException z) {
                        LOGGER.error(z.getMessage(), z);

                        String startWithBrokenZone = jsonParsers.getLocale("start_with_broken_zone", guildId);

                        EmbedBuilder errors = new EmbedBuilder();
                        errors.setColor(Color.GREEN);
                        errors.setDescription(startWithBrokenZone);

                        textChannelById.sendMessageEmbeds(errors.build()).queue();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}