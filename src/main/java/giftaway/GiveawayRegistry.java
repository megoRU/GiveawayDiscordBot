package giftaway;

import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

public class GiveawayRegistry {

  //TODO: Почистить код и в MessageGift тоже.
  // +Нужно еще написать код для удаления activeGiveaways можно взять код из Gift
  private static final Map<Long, Gift> giveaways = new HashMap<>();
  private static final Map<Long, Gift> activeGiveaways = new HashMap<>();

  public void createGiveaway(Long guidId, @NotNull Guild guild) {
    giveaways.put(guidId, new Gift(guild));
  }

  public Map<Long, Gift> getGiveaways() {
    return giveaways;
  }

  public Map<Long, Gift> getActiveGiveaways() {
    return activeGiveaways;
  }

  public Gift getGift(long userId) {
    return activeGiveaways.get(userId);
  }

  public void setGift(long guildId, Gift gift) {
    activeGiveaways.put(guildId, gift);
  }

  public boolean hasGift(long guildId) {
    return activeGiveaways.containsKey(guildId);
  }

}
