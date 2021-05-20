package threads;

import giveaway.GiveawayRegistry;
import startbot.BotStart;
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
            if (giveaway.getTIME() == null) {
                return;
            }
            if (!giveaway.getTIME().equals("null")) {
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
                BotStart.getQueue().addLast(giveaway);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}