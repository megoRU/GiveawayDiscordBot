import giveaway.GiveawayRegistry;
import startbot.BotStart;
import threads.StopGiveawayByTimer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static ExecutorService executorService;

  public static void main(String[] args) throws Exception {

    BotStart botStart = new BotStart();
    botStart.startBot();
    GiveawayRegistry.getInstance();

//    TopGGAndStatcordThread topGGAndStatcordThread = new TopGGAndStatcordThread();
//    topGGAndStatcordThread.start();

    System.out.println("23:00");

    Thread thread = new Thread(() -> {
      try {
        while (true) {
          int count = BotStart.getQueue().size();
          for (int i = 0; i < count; i++) {
            executorService = Executors.newFixedThreadPool(2);
            executorService.submit(new StopGiveawayByTimer(BotStart.getQueue().poll()));
          }
          executorService.shutdown();
          Thread.sleep(2000);
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    thread.start();
  }
}