package giveaway;

import jsonparser.JSONParsers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GiveawayRegistry {

    //Возвращает Gift по long id
    private static final ConcurrentMap<Long, Gift> activeGiveaways = new ConcurrentHashMap<>();
    //Возвращает String id message с активным Giveaway
    private static final ConcurrentMap<Long, String> idMessagesWithGiveawayButtons = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, String> channelId = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, String> messageId = new ConcurrentHashMap<>();
    private static final Map<Long, String> countWinners = new HashMap<>();
    private static final ConcurrentMap<Long, String> title = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, String> endGiveawayDate = new ConcurrentHashMap<>();
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

    public ConcurrentMap<Long, String> getIdMessagesWithGiveawayButtons() {
        return idMessagesWithGiveawayButtons;
    }

    public ConcurrentMap<Long, Gift> getActiveGiveaways() {
        return activeGiveaways;
    }

    public Map<Long, String> getMessageId() {
        return messageId;
    }

    public Map<Long, String> getTitle() {
        return title;
    }

    public ConcurrentMap<Long, String> getEndGiveawayDate() {
        return endGiveawayDate;
    }

    public Map<Long, String> getChannelId() {
        return channelId;
    }

    public Map<Long, String> getCountWinners() {
        return countWinners;
    }

}