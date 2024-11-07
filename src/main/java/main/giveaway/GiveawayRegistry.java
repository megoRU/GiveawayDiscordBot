package main.giveaway;

import main.model.entity.Scheduling;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, Giveaway> giveawayMap = new ConcurrentHashMap<>();
    private static final Map<Long, Scheduling> schedulingMap = new ConcurrentHashMap<>();
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

    public Collection<Giveaway> getAllGiveaway() {
        return giveawayMap.values();
    }

    public Collection<Scheduling> getScheduledGiveaways() {
        return schedulingMap.values();
    }

    public void removeScheduling(long guildId) {
        schedulingMap.remove(guildId);
    }

    public void putScheduling(long guildId, Scheduling scheduling) {
        schedulingMap.put(guildId, scheduling);
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

    public void removeGuildFromGiveaway(long guildId) {
        giveawayMap.remove(guildId);
    }
}