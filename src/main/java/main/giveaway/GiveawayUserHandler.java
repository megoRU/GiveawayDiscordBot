package main.giveaway;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GiveawayUserHandler {

    private final GiveawayRepositoryService giveawayRepositoryService;

    @Transactional
    public void saveUser(ActiveGiveaways activeGiveaways, List<Participant> user) {
        boolean finish = activeGiveaways.isFinish();

        if (!finish && !user.isEmpty()) {
            List<Participants> participantsList = new ArrayList<>(user.size() + 1);

            for (Participant users : user) {
                String nickname = users.nickname();
                long userId = users.userId();

                Participants participants = new Participants();
                participants.setUserId(userId);
                participants.setNickName(nickname);
                participants.setActiveGiveaways(activeGiveaways);

                participantsList.add(participants);
            }
            giveawayRepositoryService.saveParticipants(participantsList);
        }
    }
}