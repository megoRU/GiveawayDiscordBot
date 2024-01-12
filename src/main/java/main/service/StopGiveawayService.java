package main.service;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
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

                try {
                    //TODO завершать если прошёл месяц?
                    Timestamp now = Timestamp.from(Instant.now());
                    Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
                    boolean finishGiveaway = giveaway.isFinishGiveaway();
                    boolean lockEnd = giveaway.isLockEnd();
                    if (endGiveawayDate != null || finishGiveaway) {
                        if (!lockEnd && now.after(endGiveawayDate)) {
                            LOGGER.info(logMessage);
                            giveaway.stopGiveaway(countWinners);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
    }
}