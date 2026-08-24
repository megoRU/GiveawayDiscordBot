package main.threads;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.GiveawayEnds;
import main.model.entity.ActiveGiveaways;
import main.service.GiveawayRepositoryService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
public class StopGiveawayHandler {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayHandler.class.getName());
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final UpdateController updateController;

    @Transactional
    public void handleGiveaway(ActiveGiveaways activeGiveaways) {
        if (activeGiveaways != null) {
            try {
                int countWinners = activeGiveaways.getCountWinners();

                if (shouldFinishGiveaway(activeGiveaways)) {
                    GiveawayEnds giveawayEnds = new GiveawayEnds(giveawayRepositoryService);
                    giveawayEnds.stop(activeGiveaways, countWinners, updateController);
                }
            } catch (Exception e) {
                logError(e);
            }
        }
    }

    private boolean shouldFinishGiveaway(ActiveGiveaways activeGiveaways) {
        if (activeGiveaways.isFinish()) return true;

        long userIdLong = activeGiveaways.getCreatedUserId();
        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);

        ZoneId zoneOffset = ZoneId.of(zonesIdByUser);
        ZonedDateTime endInstant = activeGiveaways.getEndGiveawayDate().atZone(zoneOffset);

        return Instant.now().atZone(zoneOffset).isAfter(endInstant);
    }

    private void logError(Exception e) {
        LOGGER.log(Level.SEVERE, "An error occurred in handleGiveaway", e);
    }
}