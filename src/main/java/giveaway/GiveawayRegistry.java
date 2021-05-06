package giveaway;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import jsonparser.JSONParsers;

public class GiveawayRegistry {

  private static final Map<Long, Gift> activeGiveaways = new HashMap<>();
  private static final Map<Long, String> idMessagesWithGiveawayEmoji = new HashMap<>();
  private static final AtomicInteger giveawayCount = new AtomicInteger(0);
  private static final Map<Long, String> channelId = new HashMap<>();
  private static final Map<Long, String> messageId = new HashMap<>();
  private static final Map<Long, String> countWinners = new HashMap<>();
  private static final Map<Long, String> title = new HashMap<>();
  private static final ConcurrentMap<Long, String> endGiveawayDate = new ConcurrentHashMap<>();
  private static volatile GiveawayRegistry giveawayRegistry;
  private static final JSONParsers jsonParsers = new JSONParsers();

  public static GiveawayRegistry getInstance() {
    if (giveawayRegistry == null) {
      synchronized (GiveawayRegistry.class) {
        if (giveawayRegistry == null) {
          giveawayRegistry = new GiveawayRegistry();
        }
      }
    }
    return giveawayRegistry;
  }

  private GiveawayRegistry() {}

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
    return jsonParsers.getLocale("giveaway_registry_Error", String.valueOf(guildId));
  }

  public Map<Long, String> getMessageId() {
    return messageId;
  }

  public Map<Long, String> getTitle() {
    return title;
  }

  public Map<Long, String> getEndGiveawayDate() {
    return endGiveawayDate;
  }

  public Map<Long, String> getChannelId() {
    return channelId;
  }

  public Map<Long, String> getCountWinners() {
    return countWinners;
  }

  public void incrementGiveAwayCount() {
    giveawayCount.getAndIncrement();
  }

  public void decrementGiveAwayCount() {
    giveawayCount.decrementAndGet();
  }

  public Integer getGiveAwayCount() {
    return giveawayCount.get();
  }

  public void setGiveAwayCount(Integer value) {
    giveawayCount.set(value);
  }

}