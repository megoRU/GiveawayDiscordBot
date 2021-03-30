package threads;

import config.Config;
import giveaway.GiveawayRegistry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;

public class StopGiveawayByTimer extends Thread {

  @Override
  public void run() {
    try {
      while (true) {
        Connection connection = DriverManager.getConnection(
            Config.getGiveawayConnection(),
            Config.getGiveawayUser(),
            Config.getGiveawayPass());
        Statement statement = connection.createStatement();
        String sql = "select * from ActiveGiveaways";
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
          long guild_long_id = rs.getLong("guild_long_id");
          long channel_id_long = rs.getLong("channel_id_long");
          String count_winners = rs.getString("count_winners");
          String date_end_giveaway = rs.getString("date_end_giveaway");
          if (date_end_giveaway != null) {
            Instant timestamp = Instant.now();
            //Instant для timestamp
            Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
            OffsetDateTime timeFormDB = OffsetDateTime.parse(date_end_giveaway);

            if (specificTime.isAfter(Instant.from(timeFormDB))) {
              GiveawayRegistry.getInstance()
                  .getActiveGiveaways()
                  .get(guild_long_id)
                  .stopGift(guild_long_id, channel_id_long,
                      count_winners == null ? 1 : Integer.parseInt(count_winners));
            }
          }
        }
        statement.close();
        rs.close();
        connection.close();
        TopGGApiThread.sleep(5000);
      }
    } catch (Exception e) {
      StopGiveawayByTimer.currentThread().interrupt();
      e.printStackTrace();
    }
  }
}
