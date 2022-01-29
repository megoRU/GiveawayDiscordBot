package main.giveaway;

import main.jsonparser.JSONParsers;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class GiveawayRegistry {

    //Возвращает Gift по long id
    private static final Map<Long, Gift> activeGiveaways = new HashMap<>();
    //Возвращает String id message с активным Giveaway
    private static final Map<Long, String> idMessagesWithGiveawayButtons = new HashMap<>();
    private static final Map<Long, String> channelId = new HashMap<>();
    private static final Map<Long, String> messageId = new HashMap<>();
    private static final Map<Long, String> countWinners = new HashMap<>();
    private static final Map<Long, String> title = new HashMap<>();
    private static final Map<Long, Timestamp> endGiveawayDate = new HashMap<>();
    private static final JSONParsers jsonParsers = new JSONParsers();
    private static volatile GiveawayRegistry giveawayRegistry;

    private GiveawayRegistry() {
    }

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

    public void getGift(long guildId) {
        activeGiveaways.get(guildId);
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
        return jsonParsers.getLocale("giveaway_registry_Error", String.valueOf(guildId));
    }

    public Map<Long, String> getIdMessagesWithGiveawayButtons() {
        return idMessagesWithGiveawayButtons;
    }

    public Map<Long, Gift> getActiveGiveaways() {
        return activeGiveaways;
    }

    public Map<Long, String> getMessageId() {
        return messageId;
    }

    public Map<Long, String> getTitle() {
        return title;
    }

    public Map<Long, Timestamp> getEndGiveawayDate() {
        return endGiveawayDate;
    }

    public Map<Long, String> getChannelId() {
        return channelId;
    }

    public Map<Long, String> getCountWinners() {
        return countWinners;
    }

}