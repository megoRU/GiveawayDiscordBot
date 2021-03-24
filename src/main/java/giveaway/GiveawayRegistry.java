package giveaway;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GiveawayRegistry {

  private static final Map<Long, Gift> activeGiveaways = new HashMap<>();
  private static final Map<Long, String> idMessagesWithGiveawayEmoji = new HashMap<>();
  private static final AtomicInteger giveawayCount = new AtomicInteger(0);

  public static Map<Long, Gift> getActiveGiveaways() {
    return activeGiveaways;
  }

  public static void getGift(long userId) {
    activeGiveaways.get(userId);
  }

  public static void setGift(long guildId, Gift gift) {
    activeGiveaways.put(guildId, gift);
  }

  public static boolean hasGift(long guildId) {
    return activeGiveaways.containsKey(guildId);
  }

  public static void removeGift(long guildId) {
    activeGiveaways.remove(guildId);
  }

  public static Map<Long, String> getIdMessagesWithGiveawayEmoji() {
    return idMessagesWithGiveawayEmoji;
  }

  public static String removeGiftExceptions(long guildId) {
    activeGiveaways.remove(guildId);
    return """
        The giveaway was canceled because the bot was unable to get the ID
        your post for editing. Please try again.
        """;
  }

  public synchronized static void incrementGiveAwayCount() {
    giveawayCount.getAndIncrement();
  }

  public synchronized static void decrementGiveAwayCount() {
    giveawayCount.decrementAndGet();
  }

  public synchronized static Integer getGiveAwayCount() {
    return giveawayCount.get();
  }

}
