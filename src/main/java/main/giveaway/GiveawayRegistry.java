package main.giveaway;

import main.model.entity.Scheduling;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    public void removeScheduling(long messageId) {
        schedulingMap.remove(messageId);
    }

    public void putScheduling(long guildId, Scheduling scheduling) {
        schedulingMap.put(guildId, scheduling);
    }

    @Nullable
    public Giveaway getGiveaway(long messageId) {
        return giveawayMap.get(messageId);
    }

    public List<Giveaway> getGiveawaysByGuild(long guildId) {
        return giveawayMap.values().stream()
                .filter(giveaway -> giveaway.getGuildId() == guildId)
                .toList();
    }

    public boolean hasGiveaway(long messageId) {
        return giveawayMap.containsKey(messageId);
    }

    public void putGift(long messageId, Giveaway giveaway) {
        giveawayMap.put(messageId, giveaway);
    }

    public void removeGiveaway(long messageId) {
        giveawayMap.remove(messageId);
    }
}