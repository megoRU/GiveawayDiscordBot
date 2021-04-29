package threads;

import config.Config;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;

public class TopGGApiThread extends Thread {

  @Override
  public void run() {
    try {
      while (true) {
        DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId())
            .build();
        int serverCount = (int) BotStart.getJda().getGuildCache().size();
        TOP_GG_API.setStats(serverCount);
        TopGGApiThread.sleep(300000);
      }
    } catch (Exception e) {
      TopGGApiThread.currentThread().interrupt();
      e.printStackTrace();
    }

  }
}