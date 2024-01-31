package main.giveaway;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayRegistry {

    //GuildId Giveaway
    private static final Map<Long, Giveaway> giveawayMap = new ConcurrentHashMap<>();
    private static final Map<Long, LocalDateTime> giveawayLocalDateTime = new ConcurrentHashMap<>();
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

    public Collection<Giveaway> getGiveaways() {
        return giveawayMap.values();
    }

    @Nullable
    public Giveaway getGiveaway(long guildId) {
        return giveawayMap.get(guildId);
    }

    public boolean hasGiveaway(long guildId) {
        return giveawayMap.containsKey(guildId);
    }

    public void putGift(long guildId, Giveaway giveaway) {
        giveawayMap.put(guildId, giveaway);
    }

    public void removeGiveaway(long guildId) {
        giveawayMap.remove(guildId);
    }
}