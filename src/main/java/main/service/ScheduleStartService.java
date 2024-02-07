package main.service;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayBuilder;
import main.giveaway.GiveawayRegistry;
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
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ScheduleStartService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleStartService.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    private final SchedulingRepository schedulingRepository;
    private final GiveawayBuilder.Builder giveawayBuilder;

    public void start(JDA jda) {
        List<Scheduling> allScheduling = schedulingRepository.findAll();
        for (Scheduling scheduling : allScheduling) {
            Timestamp localTime = new Timestamp(System.currentTimeMillis());
            Timestamp dateCreateGiveaway = scheduling.getDateCreateGiveaway();

            if (localTime.after(dateCreateGiveaway)) {
                try {
                    Long channelIdLong = scheduling.getChannelIdLong();
                    Guild guildById = jda.getGuildById(scheduling.getGuildLongId());

                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelIdLong);
                        if (textChannelById != null) {
                            Long role = scheduling.getRoleIdLong();
                            Boolean isOnlyForSpecificRole = scheduling.getIsForSpecificRole();
                            long guildId = scheduling.getGuildLongId();
                            Long userIdLong = scheduling.getIdUserWhoCreateGiveaway();
                            String title = scheduling.getGiveawayTitle();
                            int countWinners = scheduling.getCountWinners();
                            String urlImage = scheduling.getUrlImage();
                            Integer minParticipants = scheduling.getMinParticipants();
                            Timestamp dateEndGiveaway = scheduling.getDateEndGiveaway();
                            Long forbiddenRole = scheduling.getForbiddenRole();

                            giveawayBuilder.setTextChannelId(channelIdLong);
                            giveawayBuilder.setUserIdLong(userIdLong);
                            giveawayBuilder.setGuildId(guildId);
                            giveawayBuilder.setTitle(title);
                            giveawayBuilder.setCountWinners(countWinners);
                            giveawayBuilder.setEndGiveawayDate(dateEndGiveaway);
                            giveawayBuilder.setRoleId(role);
                            giveawayBuilder.setForSpecificRole(isOnlyForSpecificRole);
                            giveawayBuilder.setUrlImage(urlImage);
                            giveawayBuilder.setMinParticipants(minParticipants);
                            giveawayBuilder.setForbiddenRole(forbiddenRole);

                            Giveaway giveaway = giveawayBuilder.build();

                            GiveawayRegistry instance = GiveawayRegistry.getInstance();
                            instance.putGift(guildId, giveaway);

                            if (role != null) {
                                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                                if (Objects.equals(role, guildId)) {
                                    giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                                    textChannelById.sendMessage(giftNotificationForThisRole).queue();
                                } else {
                                    textChannelById.sendMessage(giftNotificationForThisRole).queue();
                                }
                            }

                            giveaway.startGiveaway(textChannelById, false);

                            schedulingRepository.deleteById(scheduling.getGuildLongId());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}