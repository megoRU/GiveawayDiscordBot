package main.threads;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
public class StopGiveawayThread implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(StopGiveawayThread.class.getName());
    private final Giveaway giveaway;

    @Override
    public void run() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        try {
            while (true) {
                long guildId = giveaway.getGuildId();

                if (!GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                    Future<?> future = instance.getFutureTasks(guildId);
                    if (future != null) {
                        future.cancel(true);
                        instance.removeFutureTasks(guildId);
                    }
                    return;
                }
                giveaway.stopGiveaway(giveaway.getCountWinners());
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } finally {
            instance.removeFutureTasks(giveaway.getGuildId());
        }
    }
}
