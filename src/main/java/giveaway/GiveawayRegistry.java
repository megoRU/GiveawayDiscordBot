package giveaway;

import java.util.HashMap;
import java.util.Map;

public class GiveawayRegistry {

  private static final Map<Long, Gift> activeGiveaways = new HashMap<>();

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

  public String removeGiftExceptions(long guildId) {
    activeGiveaways.remove(guildId);
    return """
        The giveaway was canceled because the bot was unable to get the ID
        your post for editing. Please try again.
        """;
  }

}
