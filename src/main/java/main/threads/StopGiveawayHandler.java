package main.threads;

import main.giveaway.Giveaway;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StopGiveawayHandler {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayHandler.class.getName());

    public void handleGiveaway(Giveaway giveaway) {
        try {
            if (giveaway == null) return;

            int countWinners = giveaway.getCountWinners();
            Timestamp localTime = Timestamp.from(Instant.now());

            if (shouldFinishGiveaway(giveaway, localTime)) {
                giveaway.stopGiveaway(countWinners);
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private boolean shouldFinishGiveaway(Giveaway giveaway, Timestamp localTime) {
        Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
        if (giveaway.isLocked()) {
            return false;
        } else if (giveaway.isFinishGiveaway()) {
            return true;
        } else if (endGiveawayDate == null) {
            return false;
        }
        return localTime.after(endGiveawayDate);
    }

    private void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "An error occurred in handleGiveaway", e);
    }
}