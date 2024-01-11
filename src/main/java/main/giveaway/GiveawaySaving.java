package main.giveaway;

import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

@Service
public class GiveawaySaving {

    private static final Logger LOGGER = Logger.getLogger(GiveawaySaving.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private ActiveGiveaways activeGiveaways;

    @Autowired
    public GiveawaySaving(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
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

        activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);
        activeGiveaways.setMinParticipants(minParticipants);

        if (roleId == null || roleId == 0) activeGiveaways.setRoleIdLong(null);
        else activeGiveaways.setRoleIdLong(roleId);

        activeGiveaways.setIsForSpecificRole(isForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(userIdLong);
        activeGiveaways.setDateEndGiveaway(endGiveawayDate);

        activeGiveawayRepository.save(activeGiveaways);
    }

    public void addUser(Giveaway giveaway, final User user) {
        long guildId = giveaway.getGuildId();
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

        if (!giveaway.isUserContains(user.getId())) {
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
}