package threads;

import config.Config;
import startbot.BotStart;
import startbot.Statcord;

public class StatcordThread extends Thread {

  @Override
  public void run() {
    try {
      Statcord.start(
          BotStart.getJda().getSelfUser().getId(),
          Config.getStatcord(),
          BotStart.getJda(),
          true,
          5);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}