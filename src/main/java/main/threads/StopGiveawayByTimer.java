package main.threads;

import main.giveaway.GiveawayRegistry;

import java.sql.Timestamp;
import java.util.TimerTask;

public final class StopGiveawayByTimer extends TimerTask {

    private final Long idGuild;
    private final Timestamp time;

    public StopGiveawayByTimer(Long idGuild, Timestamp time) {
        this.idGuild = idGuild;
        this.time = time;
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
                int countWinners = GiveawayRegistry.getInstance().getCountWinners(idGuild) == null ? 1
                        : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(idGuild));

                if (countWinners < listUsersSize) {
                    GiveawayRegistry.getInstance()
                            .getGift(idGuild)
                            .stopGift(idGuild,
                                    GiveawayRegistry.getInstance().getCountWinners(idGuild)
                                            == null ? 1
                                            : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(idGuild)));
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