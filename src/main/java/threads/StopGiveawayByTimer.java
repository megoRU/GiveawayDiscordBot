package threads;

import giveaway.GiveawayRegistry;
import java.time.Instant;
import java.time.OffsetDateTime;

public class StopGiveawayByTimer extends Thread {

    private final Giveaway giveaway;

    public StopGiveawayByTimer(Giveaway giveaway) {
        this.giveaway = giveaway;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (GiveawayRegistry.getInstance().getActiveGiveaways().get(giveaway.getID_GUILD()) == null) {
                    return;
                }

                Instant timestamp = Instant.now();
                Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
                OffsetDateTime timeFormDB = OffsetDateTime.parse(giveaway.getTIME());
                if (specificTime.isAfter(Instant.from(timeFormDB))) {

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(giveaway.getID_GUILD())
                            .stopGift(giveaway.getID_GUILD(),
                                    GiveawayRegistry.getInstance().getCountWinners().get(giveaway.getID_GUILD())
                                            == null ? 1
                                            : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(giveaway.getID_GUILD())));
                    return;
                }
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}