package main.giveaway;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class GiveawayUserHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayUserHandler.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    /*
    TODO:
    Если по реакции, то оптимизация не работает, нужно вообще переписать так чтобы сразу все собирались или в коллекцию потом из неё брать.
    Типа K V - K = messageId V очередь
     */

    @Transactional
    public void saveUser(Giveaway giveaway, List<User> user) {
        long messageId = giveaway.getGiveawayData().getMessageId();
        long guildId = giveaway.getGuildId();
        boolean removed = giveaway.isRemoved();

        GiveawayData giveawayData = giveaway.getGiveawayData();

        List<User> userList = user.stream()
                .filter(users -> !giveawayData.participantContains(users.getId()))
                .toList();

        if (!removed && !userList.isEmpty()) {
            ActiveGiveaways activeGiveaways = giveawayRepositoryService.getGiveaway(messageId);
            if (activeGiveaways == null) return;

            List<Participants> participantsList = new ArrayList<>(userList.size() + 1);
            for (User users : userList) {
                LOGGER.info("Новый участник: Nick: {} UserID: {} Guild: {} MessageId {}", users.getName(), users.getId(), guildId, messageId);

                Participants participants = new Participants();
                participants.setUserId(users.getIdLong());
                participants.setNickName(users.getName());
                participants.setActiveGiveaways(activeGiveaways);

                participantsList.add(participants);
                giveawayData.addParticipant(users.getId());
            }
            giveawayRepositoryService.saveParticipants(participantsList);
        }
    }

    public void preSaveUser(Giveaway giveaway, User user) {
        boolean removed = giveaway.isRemoved();
        GiveawayData giveawayData = giveaway.getGiveawayData();

        if (!removed) {
            giveawayData.addUserToQueue(user);
            giveawayData.addParticipant(user.getId());
        }
    }
}