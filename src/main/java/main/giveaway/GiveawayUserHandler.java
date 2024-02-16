package main.giveaway;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@AllArgsConstructor
public class GiveawayUserHandler {

    private static final Logger LOGGER = Logger.getLogger(GiveawayUserHandler.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    public void saveUser(Giveaway giveaway, User... user) {
        long guildId = giveaway.getGuildId();
        boolean removed = giveaway.isRemoved();

        List<User> userList = Arrays.stream(user)
                .filter(users -> !giveaway.isUsercontainsInGiveaway(users.getId()))
                .toList();

        if (!removed) {
            ActiveGiveaways activeGiveaways = giveawayRepositoryService.getGiveaway(guildId);

            Participants[] participantsList = new Participants[userList.size()];
            for (int i = 0; i < userList.size(); i++) {
                User users = userList.get(i);

                LOGGER.info(String.format("""
                                                                
                                                                
                                Новый участник
                                Nick: %s
                                UserID: %s
                                Guild: %s
                                                                
                                """,
                        users.getName(),
                        users.getId(),
                        guildId));

                Participants participants = new Participants();
                participants.setUserIdLong(users.getIdLong());
                participants.setNickName(users.getName());
                participants.setActiveGiveaways(activeGiveaways);

                participantsList[i] = participants;
                giveaway.addUserToList(users.getId());
            }
            giveawayRepositoryService.saveParticipants(participantsList);
        }
    }
}