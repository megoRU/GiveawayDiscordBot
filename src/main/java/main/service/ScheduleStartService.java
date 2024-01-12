package main.service;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.utils.GiveawayUtils;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class ScheduleStartService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleStartService.class.getName());

    private final SchedulingRepository schedulingRepository;

    @Autowired
    public ScheduleStartService(SchedulingRepository schedulingRepository) {
        this.schedulingRepository = schedulingRepository;
    }

    public void start(JDA jda) {
        List<Scheduling> allScheduling = schedulingRepository.findAll();
        for (Scheduling scheduling : allScheduling) {
            Timestamp localTime = new Timestamp(System.currentTimeMillis());

            if (localTime.after(scheduling.getDateCreateGiveaway())) {
                try {
                    Long channelIdLong = scheduling.getChannelIdLong();
                    Guild guildById = jda.getGuildById(scheduling.getGuildLongId());

                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelIdLong);
                        if (textChannelById != null) {
                            Long role = scheduling.getRoleIdLong();
                            Boolean isOnlyForSpecificRole = scheduling.getIsForSpecificRole();
                            Long guildId = scheduling.getGuildLongId();

//                            Giveaway giveaway = new Giveaway(
//                                    scheduling.getGuildLongId(),
//                                    textChannelById.getIdLong(),
//                                    scheduling.getIdUserWhoCreateGiveaway(),
//                                    activeGiveawayRepository,
//                                    participantsRepository,
//                                    listUsersRepository,
//                                    updateController);
//
//                            GiveawayRegistry instance = GiveawayRegistry.getInstance();
//                            instance.putGift(scheduling.getGuildLongId(), giveaway);
//
//                            String formattedDate = null;
//                            if (scheduling.getDateEndGiveaway() != null) {
//                                LocalDateTime dateEndGiveaway = LocalDateTime.ofInstant(scheduling.getDateEndGiveaway().toInstant(), ZoneOffset.UTC);
//                                formattedDate = dateEndGiveaway.format(GiveawayUtils.FORMATTER);
//                            }
//
//                            if (role != null && isOnlyForSpecificRole) {
//                                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
//                                if (Objects.equals(role, guildId)) {
//                                    giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
//                                    textChannelById.sendMessage(giftNotificationForThisRole).queue();
//                                } else {
//                                    textChannelById.sendMessage(giftNotificationForThisRole).queue();
//                                }
//                            }
//
//                            giveaway.startGiveaway(
//                                    textChannelById,
//                                    scheduling.getGiveawayTitle(),
//                                    scheduling.getCountWinners(),
//                                    formattedDate,
//                                    scheduling.getRoleIdLong(),
//                                    scheduling.getIsForSpecificRole(),
//                                    scheduling.getUrlImage(),
//                                    false,
//                                    scheduling.getMinParticipants());
//
//                            schedulingRepository.deleteById(scheduling.getGuildLongId());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}