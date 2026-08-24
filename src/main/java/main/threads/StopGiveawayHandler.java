package main.threads;

import main.config.BotStart;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StopGiveawayHandler {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayHandler.class.getName());

    @Transactional
    public void handleGiveaway(Giveaway giveaway) {
        if (giveaway != null) {

            GiveawayData giveawayData = giveaway.getGiveawayData();

            try {
                int countWinners = giveawayData.getCountWinners();

                if (shouldFinishGiveaway(giveaway)) {
                    giveaway.stopGiveaway(countWinners);
                }
            } catch (Exception e) {
                logError(e);
            }
        }
    }

    private boolean shouldFinishGiveaway(Giveaway giveaway) {
        if (giveaway.isLocked()) return false;
        if (giveaway.isFinishGiveaway()) return true;

        long userIdLong = giveaway.getUserIdLong();
        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);

        ZoneId zoneOffset = ZoneId.of(zonesIdByUser);
        ZonedDateTime endInstant = giveaway.getGiveawayData().getEndGiveawayDate().atZone(zoneOffset);

        return Instant.now().atZone(zoneOffset).isAfter(endInstant);
    }

    private void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "An error occurred in handleGiveaway", e);
    }
}