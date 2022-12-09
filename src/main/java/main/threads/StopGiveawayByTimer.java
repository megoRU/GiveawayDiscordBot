package main.threads;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;

import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public final class StopGiveawayByTimer extends TimerTask {

    private final Long idGuild;
    private static final Logger LOGGER = Logger.getLogger(StopGiveawayByTimer.class.getName());
    private final CountDownLatch latch = new CountDownLatch(1);

    public StopGiveawayByTimer(Long idGuild) {
        this.idGuild = idGuild;
    }

    @Override
    public void run() {
        try {
            while (true) {
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                Giveaway giveaway = instance.getGiveaway(idGuild);
                if (giveaway == null) return;

//                synchronized (this) {
//                    System.out.println();
//                    System.out.println(idGuild);
//                    System.out.println("LocalDateTime.now() " + LocalDateTime.now());
//                    System.out.println("TIME.toLocalDateTime() " + time.toLocalDateTime());
//                    System.out.println(LocalDateTime.now().isAfter(time.toLocalDateTime()));
//                }

                latch.await();
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
                    return;
                }

                Thread.sleep(3600000L * 24L);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void countDown() {
        latch.countDown();
    }

    public int getCountDown() {
        return (int) latch.getCount();
    }
}