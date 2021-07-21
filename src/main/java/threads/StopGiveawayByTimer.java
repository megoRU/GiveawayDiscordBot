package threads;

import giveaway.GiveawayRegistry;

import java.time.Instant;
import java.time.OffsetDateTime;

public class StopGiveawayByTimer extends Thread {

    private final Long ID_GUILD;
    private final String TIME;

    public StopGiveawayByTimer(Giveaway giveaway) {
        ID_GUILD = giveaway.getID_GUILD();
        TIME = giveaway.getTIME();
    }

    @Override
    public void run() {
        try {
            while (true) {

                if (GiveawayRegistry.getInstance().getActiveGiveaways().get(getID_GUILD()) == null) {
                    return;
                }

                Instant timestamp = Instant.now();
                Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
                OffsetDateTime timeFormDB = OffsetDateTime.parse(getTIME());

                if (specificTime.isAfter(Instant.from(timeFormDB))) {

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(getID_GUILD())
                            .stopGift(getID_GUILD(),
                                    GiveawayRegistry.getInstance().getCountWinners().get(getID_GUILD())
                                            == null ? 1
                                            : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(getID_GUILD())));
                    return;
                }

                Thread.sleep(2000);
            }

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public Long getID_GUILD() {
        return ID_GUILD;
    }

    public String getTIME() {
        return TIME;
    }
}