package main.giveaway;

import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class GiveawaySaving {

    private static final Logger LOGGER = Logger.getLogger(GiveawaySaving.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private ActiveGiveaways activeGiveaways;

    @Autowired
    public GiveawaySaving(ActiveGiveawayRepository activeGiveawayRepository,
                          ParticipantsRepository participantsRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    public void create(Giveaway giveaway, Message message) {
        Long roleId = giveaway.getRoleId();
        String title = giveaway.getTitle();
        long guildId = giveaway.getGuildId();
        int countWinners = giveaway.getCountWinners();
        int minParticipants = giveaway.getMinParticipants();
        boolean isForSpecificRole = giveaway.isForSpecificRole();
        long userIdLong = giveaway.getUserIdLong();
        String urlImage = giveaway.getUrlImage();
        Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
        long messageId = message.getIdLong();
        long channelId = message.getChannel().getIdLong();
        Long forbiddenRole = giveaway.getForbiddenRole();

        activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(messageId);
        activeGiveaways.setChannelIdLong(channelId);
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);
        activeGiveaways.setMinParticipants(minParticipants);
        activeGiveaways.setRoleIdLong(roleId);
        activeGiveaways.setForbiddenRole(forbiddenRole);

        activeGiveaways.setIsForSpecificRole(isForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);
        activeGiveaways.setDateEndGiveaway(endGiveawayDate);

        activeGiveaways = activeGiveawayRepository.saveAndFlush(activeGiveaways);
    }

    public void addUser(Giveaway giveaway, final User user) {
        long guildId = giveaway.getGuildId();

        if (!giveaway.isUserContains(user.getId())) {
            LOGGER.info(String.format(
                    """
                            \nНовый участник
                            Nick: %s
                            UserID: %s
                            Guild: %s
                            """,
                    user.getName(),
                    user.getId(),
                    guildId));
            giveaway.addUserToList(user.getId());

            if (activeGiveaways == null) {
                Optional<ActiveGiveaways> optionalActiveGiveaways = activeGiveawayRepository.findById(guildId);
                if (optionalActiveGiveaways.isPresent()) activeGiveaways = optionalActiveGiveaways.get();
                else return;
            }

            //Add user to Collection
            Participants participants = new Participants();
            participants.setUserIdLong(user.getIdLong());
            participants.setNickName(user.getName());
            participants.setActiveGiveaways(activeGiveaways);

            giveaway.addParticipantToList(participants);
        }
    }

    public void saveParticipants(long guildId, ConcurrentLinkedQueue<Participants> participantsList) {
        GiveawayRegistry giveawayRegistry = GiveawayRegistry.getInstance();
        Giveaway giveaway = giveawayRegistry.getGiveaway(guildId);
        if (giveaway == null) return;

        if (!participantsList.isEmpty()) {
            List<Participants> arrayList = new ArrayList<>(150);
            while (!participantsList.isEmpty()) {
                Participants poll = participantsList.poll();
                if (poll != null) {
                    arrayList.add(poll);
                }
            }
            try {
                participantsRepository.saveAll(arrayList);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }
}