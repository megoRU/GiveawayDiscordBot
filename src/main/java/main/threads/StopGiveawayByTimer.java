package main.threads;

import main.giveaway.GiveawayRegistry;

import java.util.TimerTask;
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
            while (true) {
                if (!GiveawayRegistry.getInstance().hasGift(idGuild)) return;

//                synchronized (this) {
//                    System.out.println();
//                    System.out.println(idGuild);
//                    System.out.println("LocalDateTime.now() " + LocalDateTime.now());
//                    System.out.println("TIME.toLocalDateTime() " + time.toLocalDateTime());
//                    System.out.println(LocalDateTime.now().isAfter(time.toLocalDateTime()));
//                }

                int listUsersSize = GiveawayRegistry.getInstance().getGift(idGuild).getListUsersSize();
                int countWinners = GiveawayRegistry.getInstance().getCountWinners(idGuild);


                LOGGER.info("\nlistUsersSize: " + listUsersSize);
                LOGGER.info("\ncountWinners: " + countWinners);

                //TODO завершать если прошёл месяц?
                if (listUsersSize < 2 || countWinners < listUsersSize) {
                    GiveawayRegistry.getInstance().getGift(idGuild).stopGift(idGuild, countWinners);
                    return;
                }

                Thread.sleep(30000L);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

}