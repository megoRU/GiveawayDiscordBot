package main.service;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class GiveawayRepositoryService {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;

    @Transactional
    public void saveGiveaway(ActiveGiveaways activeGiveaways) {
        activeGiveawayRepository.save(activeGiveaways);
    }

    @Transactional
    public void setFinishGiveaway(long messageId) {
        activeGiveawayRepository.updateFinishGiveaway(messageId);
    }

    @Transactional
    @Nullable
    public ActiveGiveaways getGiveaway(long messageId) {
        return activeGiveawayRepository.findByMessageId(messageId);
    }

    @Transactional
    public void deleteGiveaway(long messageId) {
        activeGiveawayRepository.deleteByMessageId(messageId);
    }

    @Transactional
    public void saveParticipants(List<Participants> participants) {
        participantsRepository.saveAll(participants);
    }

    @Transactional
    public List<Participants> findAllParticipants(long messageId) {
        return participantsRepository.findParticipantsByActiveGiveaways(messageId);
    }

    @Transactional
    public void backupAllParticipants(long messageId) {
        listUsersRepository.saveAllParticipantsToUserList(messageId);
    }
}