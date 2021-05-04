import giveaway.GiveawayRegistry;
import startbot.BotStart;
import threads.StopGiveawayByTimer;
import threads.TopGGAndStatcordThread;

public class Main {

  public static void main(String[] args) throws Exception {

    BotStart botStart = new BotStart();
    botStart.startBot();
    GiveawayRegistry.getInstance();

    TopGGAndStatcordThread topGGAndStatcordThread = new TopGGAndStatcordThread();
    topGGAndStatcordThread.start();

    System.out.println("23:00");

    StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer();
    stopGiveawayByTimer.start();
  }

}
