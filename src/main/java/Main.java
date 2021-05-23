import giveaway.GiveawayRegistry;
import startbot.BotStart;
import threads.StopGiveawayByTimer;
import threads.TopGGAndStatcordThread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static ExecutorService executorService = Executors.newFixedThreadPool(2);

  public static void main(String[] args) throws Exception {

    BotStart botStart = new BotStart();
    botStart.startBot();
    GiveawayRegistry.getInstance();

//    TopGGAndStatcordThread topGGAndStatcordThread = new TopGGAndStatcordThread();
//    topGGAndStatcordThread.start();

    System.out.println("0:48");

    Thread thread = new Thread(() -> {
      try {
        while (true) {
          int count = BotStart.getQueue().size();
          for (int i = 0; i < count; i++) {
            executorService.submit(new StopGiveawayByTimer(BotStart.getQueue().poll()));
          }
          Thread.sleep(2000);
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    thread.start();
  }
}