package main.service;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UploadGiveawaysService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadGiveawaysService.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;

    public void uploadGiveaways(UpdateController updateController) {
        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll();

        for (ActiveGiveaways activeGiveaways : activeGiveawaysList) {
            try {
                long guild_long_id = activeGiveaways.getGuildId();
                long channel_long_id = activeGiveaways.getChannelId();
                int count_winners = activeGiveaways.getCountWinners();
                long message_id_long = activeGiveaways.getMessageId();
                String giveaway_title = activeGiveaways.getTitle();
                Instant date_end_giveaway = activeGiveaways.getEndGiveawayDate();
                Long role_id_long = activeGiveaways.getRoleId();
                boolean is_for_specific_role = Optional.ofNullable(activeGiveaways.getIsForSpecificRole()).orElse(false);
                String url_image = activeGiveaways.getUrlImage();
                long id_user_who_create_giveaway = activeGiveaways.getCreatedUserId();
                Integer min_participants = activeGiveaways.getMinParticipants();
                boolean finishGiveaway = activeGiveaways.isFinish();

                Set<Long> participantsList = activeGiveaways.getParticipants()
                        .stream()
                        .map(Participants::getUserId)
                        .collect(Collectors.toSet());

                Giveaway giveaway = new Giveaway(guild_long_id,
                        channel_long_id,
                        id_user_who_create_giveaway,
                        finishGiveaway,
                        true,
                        message_id_long,
                        count_winners,
                        role_id_long,
                        is_for_specific_role,
                        url_image,
                        giveaway_title == null ? "Giveaway" : giveaway_title,
                        date_end_giveaway,
                        min_participants == null ? 1 : min_participants,
                        giveawayRepositoryService,
                        updateController);

                giveaway.setParticipantsList(participantsList);

                //Снимаем блокировку
                giveaway.setLocked(false);

                if (finishGiveaway) {
                    giveaway.stopGiveaway(count_winners);
                }
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null && (message.contains("10008: Unknown Message")
                        || message.contains("Missing permission: MESSAGE_SEND")
                        || message.contains("Missing permission: VIEW_CHANNEL"))) {
                    LOGGER.info("Delete Giveaway {}", activeGiveaways.getMessageId());
                    updateController.getGiveawayRepositoryService().deleteGiveaway(activeGiveaways.getMessageId());
                } else {
                    LOGGER.error(e != null ? e.getMessage() : "Error uploading giveaway", e);
                }
            }
        }
        System.out.println("uploadGiveaways()");
    }
}
