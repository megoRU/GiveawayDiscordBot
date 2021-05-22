package threads;

import config.Config;
import net.dv8tion.jda.api.entities.Activity;
import org.discordbots.api.client.DiscordBotListAPI;
import startbot.BotStart;
import startbot.Statcord;

public class TopGGAndStatcordThread extends Thread {

  private boolean isLaunched;
  public static int serverCount;

  @Override
  public void run() {
    try {
      while (true) {
        DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId())
            .build();
        serverCount = (int) BotStart.getJda().getGuildCache().size();
        TOP_GG_API.setStats(serverCount);
        BotStart.getJda().getPresence().setActivity(Activity.playing(BotStart.activity
                        + BotStart.version
                        + " | "
                        + TopGGAndStatcordThread.serverCount + " guilds"));

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