package threads;

import giveaway.GiveawayRegistry;
import java.time.Instant;
import java.time.OffsetDateTime;

public class StopGiveawayByTimer extends Thread {

  @Override
  public void run() {
    try {
      while (true) {
        GiveawayRegistry.getInstance().getEndGiveawayDate().forEach((k, v) -> {
          if (!v.equals("null")) {
            Instant timestamp = Instant.now();
            //Instant для timestamp
            Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
            OffsetDateTime timeFormDB = OffsetDateTime.parse(v);

            if (specificTime.isAfter(Instant.from(timeFormDB))) {
              GiveawayRegistry.getInstance()
                  .getActiveGiveaways()
                  .get(k)
                  .stopGift(k,
                      Long.parseLong(GiveawayRegistry.getInstance().getChannelId().get(k)),
                      GiveawayRegistry.getInstance().getCountWinners().get(k)
                          == null ? 1 : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(k)));
            }
          }
        });
        StopGiveawayByTimer.sleep(3000);
      }
    } catch (Exception e) {
      StopGiveawayByTimer.currentThread().interrupt();
      e.printStackTrace();
    }
  }
}