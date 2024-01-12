package main.service;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public final class StopGiveawayService {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayService.class.getName());

    public void stop() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Collection<Giveaway> giveawayList = instance.getGiveaways();
        giveawayList.forEach(giveaway -> {
            if (giveaway != null) {
                int listUsersSize = giveaway.getListUsersSize();
                int countWinners = giveaway.getCountWinners();
                long guildId = giveaway.getGuildId();
                String logMessage = String.format(
                        """
                                \n
                                Guild ID: %s
                                ListUsersSize: %s
                                CountWinners: %s
                                """, guildId, listUsersSize, countWinners);
                LOGGER.info(logMessage);
                try {
                    //TODO завершать если прошёл месяц?
                    if (countWinners <= listUsersSize) {
                        giveaway.stopGiveaway(countWinners);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
    }
}