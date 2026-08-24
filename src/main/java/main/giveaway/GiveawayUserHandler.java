package main.giveaway;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.entities.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GiveawayUserHandler {

    private final GiveawayRepositoryService giveawayRepositoryService;

    @Transactional
    public void saveUser(Giveaway giveaway, List<ParticipantDTO> user) {
        long messageId = giveaway.getMessageId();
        boolean removed = giveaway.isRemoved();

        if (!removed && !user.isEmpty()) {
            ActiveGiveaways activeGiveaways = giveawayRepositoryService.getGiveaway(messageId);
            if (activeGiveaways == null) return;

            List<Participants> participantsList = new ArrayList<>(user.size() + 1);
            for (ParticipantDTO users : user) {
                String nickname = users.getNickname();
                long userId = users.getUserId();

                Participants participants = new Participants();
                participants.setUserId(userId);
                participants.setNickName(nickname);
                participants.setActiveGiveaways(activeGiveaways);

                participantsList.add(participants);
                giveaway.addParticipant(userId);
            }
            giveawayRepositoryService.saveParticipants(participantsList);
        }
    }

    public void preSaveUser(Giveaway giveaway, User user) {
        boolean removed = giveaway.isRemoved();

        if (!removed) {
            giveaway.addUserToQueue(user);
            giveaway.addParticipant(user.getIdLong());
        }
    }
}