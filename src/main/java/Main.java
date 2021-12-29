import giveaway.GiveawayRegistry;
import startbot.BotStart;
import threads.StopGiveawayByTimer;
import threads.TopGGAndStatcordThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static ExecutorService executorService;

    public static void main(String[] args) throws Exception {

        GiveawayRegistry.getInstance();
        BotStart botStart = new BotStart();
        botStart.startBot();

        TopGGAndStatcordThread topGGAndStatcordThread = new TopGGAndStatcordThread();
        topGGAndStatcordThread.start();

        System.out.println("02:36");

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    executorService = Executors.newFixedThreadPool(2);
                    int count = BotStart.getQueue().size();

                    for (int i = 0; i < count; i++) {
                        executorService.submit(new StopGiveawayByTimer(BotStart.getQueue().poll()));
                    }
                    executorService.shutdown();

                    Thread.sleep(2000);
                }

            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
        thread.start();
    }
}