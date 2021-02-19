package threads;

import config.Config;
import startbot.BotStart;
import startbot.Statcord;

public class StatcordThread extends Thread {

  @Override
  public void run() {
    try {
      Statcord.start(
          BotStart.jda.getSelfUser().getId(),
          Config.getStatcrord(),
          BotStart.jda,
          true,
          5);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}