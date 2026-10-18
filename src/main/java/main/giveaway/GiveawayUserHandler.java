package main.giveaway;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class GiveawayUserHandler {

    private final GiveawayRepositoryService giveawayRepositoryService;

    @Transactional
    public void saveUser(ActiveGiveaways activeGiveaways, List<Participant> user) {
        if (user.isEmpty()) return;

        Set<Long> savedUserIds = new HashSet<>();
        for (Participants participant : giveawayRepositoryService.findAllParticipants(activeGiveaways.getMessageId())) {
            savedUserIds.add(participant.getUserId());
        }

        List<Participants> participantsList = new ArrayList<>(user.size());
        for (Participant userParticipant : user) {
            if (!savedUserIds.add(userParticipant.userId())) continue;

            Participants participant = new Participants();
            participant.setUserId(userParticipant.userId());
            participant.setNickName(userParticipant.nickname());
            participant.setActiveGiveaways(activeGiveaways);
            participantsList.add(participant);
        }

        if (!participantsList.isEmpty()) {
            giveawayRepositoryService.saveParticipants(participantsList);
        }
    }
}
