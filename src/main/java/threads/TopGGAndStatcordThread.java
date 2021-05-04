package threads;

import config.Config;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;
import startbot.Statcord;

public class TopGGAndStatcordThread extends Thread {

  private boolean isLaunched;

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

        if (!isLaunched) {
          Statcord.start(
              BotStart.getJda().getSelfUser().getId(),
              Config.getStatcord(),
              BotStart.getJda(),
              true,
              3);
          isLaunched = true;
        }
        TopGGAndStatcordThread.sleep(180000);
      }
    } catch (Exception e) {
      TopGGAndStatcordThread.currentThread().interrupt();
      e.printStackTrace();
    }

  }
}