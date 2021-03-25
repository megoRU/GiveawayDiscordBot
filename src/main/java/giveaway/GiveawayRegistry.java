package giveaway;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GiveawayRegistry {

  private static final Map<Long, Gift> activeGiveaways = new HashMap<>();
  private static final Map<Long, String> idMessagesWithGiveawayEmoji = new HashMap<>();
  private static final AtomicInteger giveawayCount = new AtomicInteger(0);
  private static final Map<Long, String> messageId = new HashMap<>();
  private static final Map<Long, String> title = new HashMap<>();
  private static volatile GiveawayRegistry instance;

  public static GiveawayRegistry getInstance() {
    GiveawayRegistry localInstance = instance;
    if (localInstance == null) {
      synchronized (GiveawayRegistry.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = localInstance = new GiveawayRegistry();
        }
      }
    }
    return localInstance;
  }

  public Map<Long, Gift> getActiveGiveaways() {
    return activeGiveaways;
  }

  public void getGift(long userId) {
    activeGiveaways.get(userId);
  }

  public void setGift(long guildId, Gift gift) {
    activeGiveaways.put(guildId, gift);
  }

  public boolean hasGift(long guildId) {
    return activeGiveaways.containsKey(guildId);
  }

  public void removeGift(long guildId) {
    activeGiveaways.remove(guildId);
  }

  public Map<Long, String> getIdMessagesWithGiveawayEmoji() {
    return idMessagesWithGiveawayEmoji;
  }

  public String removeGiftExceptions(long guildId) {
    activeGiveaways.remove(guildId);
    return """
        The giveaway was canceled because the bot was unable to get the ID
        your post for editing. Please try again.
        """;
  }

  public Map<Long, String> getMessageId() {
    return messageId;
  }

  public Map<Long, String> getTitle() {
    return title;
  }

  public synchronized void incrementGiveAwayCount() {
    giveawayCount.getAndIncrement();
  }

  public synchronized void decrementGiveAwayCount() {
    giveawayCount.decrementAndGet();
  }

  public synchronized Integer getGiveAwayCount() {
    return giveawayCount.get();
  }

}
