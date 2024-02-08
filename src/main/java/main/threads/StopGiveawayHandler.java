package main.threads;

import main.giveaway.Giveaway;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StopGiveawayHandler {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayHandler.class.getName());

    public void handlerGiveaway(Giveaway giveaway) {
        try {
            if (giveaway == null) return;
            int countWinners = giveaway.getCountWinners();

            Timestamp localTime = Timestamp.from(Instant.now());
            Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
            if ((localTime.after(endGiveawayDate) || giveaway.isFinishGiveaway()) && !giveaway.isLocked()) {
                giveaway.stopGiveaway(countWinners);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}