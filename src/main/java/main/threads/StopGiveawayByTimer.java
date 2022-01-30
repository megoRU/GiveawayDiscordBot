package main.threads;

import lombok.Getter;
import main.giveaway.GiveawayRegistry;

import java.sql.Timestamp;
import java.time.*;

@Getter
public class StopGiveawayByTimer implements Runnable {

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
                if (!GiveawayRegistry.getInstance().hasGift(getID_GUILD())) {
                    return;
                }

                if (LocalDateTime.now().isAfter(TIME.toLocalDateTime())) {

                    GiveawayRegistry.getInstance()
                            .getGift(getID_GUILD())
                            .stopGift(getID_GUILD(),
                                    GiveawayRegistry.getInstance().getCountWinners(getID_GUILD())
                                            == null ? 1
                                            : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(getID_GUILD())));
                }

                Thread.sleep(5000L);
            }

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

}