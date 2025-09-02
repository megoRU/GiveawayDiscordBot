package main.threads;

import main.config.BotStart;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;

import java.sql.Timestamp;
import java.time.*;
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
        System.out.println(zonesIdByUser);
        ZoneOffset zoneOffset = ZoneOffset.of(zonesIdByUser);

        LocalDateTime localDateTime = localTime.toInstant().atOffset(zoneOffset).toLocalDateTime();

        LocalDateTime endLocalDateTime = endGiveawayDate.atZone(zoneOffset).toLocalDateTime();
        ZonedDateTime endLocalDateTime2 = endGiveawayDate.atOffset(zoneOffset).toZonedDateTime();

        System.out.println("localTime.toInstant: " + localTime.toInstant().atZone(zoneOffset).toInstant());
        System.out.println("localDateTime: " + localDateTime);
        System.out.println("endLocalDateTime: " + endLocalDateTime);
        System.out.println("endLocalDateTime2: " + endLocalDateTime2);
        System.out.println("endGiveawayDate: " + endGiveawayDate);


        ZonedDateTime endInstant = giveaway.getGiveawayData().getEndGiveawayDate().toInstant().atZone(zoneOffset);
        System.out.println();

        System.out.println("Instant.now(): " + Instant.now().atZone(zoneOffset));
        System.out.println("endInstant: " + endInstant);

        return Instant.now().atZone(zoneOffset).isAfter(endInstant);
    }

    private void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "An error occurred in handleGiveaway", e);
    }
}