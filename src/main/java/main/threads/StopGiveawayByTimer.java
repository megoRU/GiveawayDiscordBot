package main.threads;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StopGiveawayByTimer extends TimerTask {

    private final Long idGuild;
    private static final Logger LOGGER = Logger.getLogger(StopGiveawayByTimer.class.getName());

    public StopGiveawayByTimer(Long idGuild) {
        this.idGuild = idGuild;
    }

    @Override
    public void run() {
        try {
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Giveaway giveaway = instance.getGiveaway(idGuild);
            if (giveaway == null) return;
            int listUsersSize = giveaway.getListUsersSize();
            int countWinners = giveaway.getCountWinners();
            String logMessage = String.format(
                    """
                            \n
                            Guild ID: %s
                            ListUsersSize: %s
                            CountWinners: %s
                            """, this.idGuild, listUsersSize, countWinners);
            LOGGER.info(logMessage);

            //TODO завершать если прошёл месяц?
            if (listUsersSize < 2 || countWinners < listUsersSize) {
                giveaway.stopGiveaway(countWinners);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}