package main.service;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUserHandler;
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
                ConcurrentLinkedQueue<User> collectionQueue = giveawayData.getCollectionQueue();
                LOGGER.info("Queue before poll: {}", collectionQueue);

                if (collectionQueue != null && !collectionQueue.isEmpty()) {
                    List<User> userList = new ArrayList<>();

                    for (int i = 0; i < collectionQueue.size(); i++) {
                        userList.add(collectionQueue.poll());
                    }

                    LOGGER.info("Queue after poll: {}", collectionQueue);

                    giveawayUserHandler.saveUser(giveaway, userList);

                    //
                    //2025-03-07T08:56:55.185Z
                    //2025-03-07T09:00:17.498Z
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }
}