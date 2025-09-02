package main.threads;

import main.config.BotStart;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StopGiveawayHandler {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayHandler.class.getName());

    public void handleGiveaway(Giveaway giveaway) {
        try {
            if (giveaway == null) return;
            GiveawayData giveawayData = giveaway.getGiveawayData();
            int countWinners = giveawayData.getCountWinners();

            Timestamp localTime = Timestamp.from(Instant.now());

            if (shouldFinishGiveaway(giveaway, localTime)) {
                giveaway.stopGiveaway(countWinners);
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private boolean shouldFinishGiveaway(Giveaway giveaway, Timestamp localTime) {
        if (giveaway.isLocked()) return false;
        if (giveaway.isFinishGiveaway()) return true;
        long userIdLong = giveaway.getUserIdLong();
        Instant endGiveawayDate = giveaway.getGiveawayData().getEndGiveawayDate().toInstant();
        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
        System.out.println(zonesIdByUser.replace("UTC", ""));
        ZoneOffset zoneOffset = ZoneOffset.of(zonesIdByUser.replace("UTC", ""));

        System.out.println(localTime.toInstant().atOffset(zoneOffset).toLocalDateTime());
        System.out.println(endGiveawayDate.atOffset(zoneOffset).toLocalDateTime());
        System.out.println("endGiveawayDate: " + endGiveawayDate);

        // localTime должен быть тоже в UTC
        return localTime.toInstant().atOffset(zoneOffset).isAfter(endGiveawayDate.atOffset(zoneOffset));
    }

    private void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "An error occurred in handleGiveaway", e);
    }
}