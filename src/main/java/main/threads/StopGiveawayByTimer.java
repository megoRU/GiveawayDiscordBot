package main.threads;

import main.giveaway.GiveawayRegistry;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public final class StopGiveawayByTimer implements Runnable {

    private final Long ID_GUILD;
    private final Timestamp TIME;

    public StopGiveawayByTimer(Giveaway giveaway) {
        ID_GUILD = giveaway.getID_GUILD();
        TIME = giveaway.getTIME();
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (!GiveawayRegistry.getInstance().hasGift(ID_GUILD)) return;

//                synchronized (BotStartConfig.getQueue()) {
//                    System.out.println();
//                    System.out.println(ID_GUILD);
//                    System.out.println("LocalDateTime.now() " + LocalDateTime.now());
//                    System.out.println("TIME.toLocalDateTime() " + TIME.toLocalDateTime());
//                    System.out.println(LocalDateTime.now().isAfter(TIME.toLocalDateTime()));
//                }

                if (LocalDateTime.now().isAfter(TIME.toLocalDateTime())) {

                    int listUsersSize = GiveawayRegistry.getInstance().getGift(ID_GUILD).getListUsersSize();
                    int countWinners = GiveawayRegistry.getInstance().getCountWinners(ID_GUILD) == null ? 1
                            : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(ID_GUILD));

                    if (countWinners < listUsersSize) {
                        GiveawayRegistry.getInstance()
                                .getGift(ID_GUILD)
                                .stopGift(ID_GUILD,
                                        GiveawayRegistry.getInstance().getCountWinners(ID_GUILD)
                                                == null ? 1
                                                : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(ID_GUILD)));
                        return;
                    }
                }
                Thread.sleep(5000L);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

}