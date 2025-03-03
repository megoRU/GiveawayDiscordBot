package main.service;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ScheduleStartService {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleStartService.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final SchedulingRepository schedulingRepository;

    public void scheduleStart(UpdateController updateController, JDA jda) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Collection<Scheduling> scheduledGiveaways = instance.getScheduledGiveaways();

        for (Scheduling scheduling : scheduledGiveaways) {
            Timestamp localTime = new Timestamp(System.currentTimeMillis());

            if (localTime.after(scheduling.getDateCreateGiveaway())) {
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

                            Giveaway giveaway = new Giveaway(
                                    scheduling.getGuildId(),
                                    textChannelById.getIdLong(),
                                    scheduling.getCreatedUserId(),
                                    giveawayRepositoryService,
                                    updateController);

                            String formattedDate = null;
                            if (scheduling.getDateEnd() != null) {
                                LocalDateTime dateEndGiveaway = LocalDateTime.ofInstant(scheduling.getDateEnd().toInstant(), ZoneOffset.UTC);
                                formattedDate = dateEndGiveaway.format(GiveawayUtils.FORMATTER);
                            }

                            if (role != null && isOnlyForSpecificRole) {
                                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                                if (Objects.equals(role, guildIdLong)) {
                                    giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                                    textChannelById.sendMessage(giftNotificationForThisRole).submit().get();
                                } else {
                                    textChannelById.sendMessage(giftNotificationForThisRole).submit().get();
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
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
