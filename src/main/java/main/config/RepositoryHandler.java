package main.config;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Language;
import main.model.entity.Notification;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.NotificationRepository;
import main.model.repository.ParticipantsRepository;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
public class RepositoryHandler {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationRepository notificationRepository;

    //ActiveGiveawayRepository
    public void deleteActiveGiveaway(long guildIdLong) {
        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
    }

    public List<ActiveGiveaways> getAllActiveGiveaway() {
        return activeGiveawayRepository.getAllActiveGiveaways();
    }

    public ActiveGiveaways getGiveawayByGuildId(long guildIdLong) {
        return activeGiveawayRepository.getActiveGiveawaysByGuildIdLong(guildIdLong);
    }

    public void saveActiveGiveaway(ActiveGiveaways activeGiveaway) {
        activeGiveawayRepository.saveAndFlush(activeGiveaway);
    }
    //

    //ParticipantsRepository
    public List<Participants> getParticipants(long guildIdLong) {
       return participantsRepository.getParticipantsByGuildIdLong(guildIdLong);
    }

    public Participants getParticipant(long guildIdLong, long userIdLong) {
        return participantsRepository.getParticipant(guildIdLong, userIdLong);
    }

    public void saveParticipant(Set<Participants> participants) {
        participantsRepository.saveAllAndFlush(participants);
    }

    //NotificationRepository
    public void saveNotification(Notification notification) {
        notificationRepository.save(notification);
    }
    //

    //LanguageRepository
    public void saveLanguage(Language language) {
        languageRepository.save(language);
    }

    public void deleteLanguage(String guildIdLong) {
        languageRepository.deleteLanguage(guildIdLong);
    }
    //
}
