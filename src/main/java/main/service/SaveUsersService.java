package main.service;

import lombok.AllArgsConstructor;
import main.giveaway.*;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@AllArgsConstructor
public class SaveUsersService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SaveUsersService.class.getName());

    private final static GiveawayRegistry giveawayRegistry = GiveawayRegistry.getInstance();
    private final GiveawayRepositoryService giveawayRepositoryService;

    public void saveParticipants() {
        Collection<Giveaway> giveawayCollection = giveawayRegistry.getAllGiveaway();
        GiveawayUserHandler giveawayUserHandler = new GiveawayUserHandler(giveawayRepositoryService);

        giveawayCollection.forEach(giveaway -> {
            try {
                GiveawayData giveawayData = giveaway.getGiveawayData();
                ConcurrentLinkedQueue<ParticipantDTO> collectionQueue = giveawayData.getCollectionQueue();

                if (collectionQueue != null && !collectionQueue.isEmpty()) {
                    List<ParticipantDTO> userList = new ArrayList<>();

                    for (int i = 0; i < collectionQueue.size(); i++) {
                        userList.add(collectionQueue.poll());
                    }

                    giveawayUserHandler.saveUser(giveaway, userList);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }
}