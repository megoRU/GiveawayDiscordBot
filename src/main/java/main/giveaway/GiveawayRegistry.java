package main.giveaway;

import main.threads.StopGiveawayByTimer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, Giveaway> giveawayMap = new HashMap<>();
    private static final Map<Long, Giveaway.GiveawayTimerStorage> giveawayTimer = new HashMap<>();
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

    @Nullable
    public Giveaway.GiveawayTimerStorage getGiveawayTimer(long guildId) {
        return giveawayTimer.get(guildId);
    }

    public static Collection<Giveaway> getAllGiveaway() {
        return giveawayMap.values();
    }

    @Nullable
    public Giveaway getGiveaway(long guildId) {
        return giveawayMap.get(guildId);
    }

    public boolean hasGiveaway(long guildId) {
        return giveawayMap.containsKey(guildId);
    }

    public void cancelGiveawayTimer(long guildId) {
        Giveaway.GiveawayTimerStorage giveawayTimerStorage = getGiveawayTimer(guildId);
        if (giveawayTimerStorage != null) {
            giveawayTimerStorage.stopGiveawayByTimer().cancel();
            giveawayTimerStorage.timer().cancel();
        }
    }

    public void putGift(long guildId, Giveaway giveaway) {
        giveawayMap.put(guildId, giveaway);
    }

    public void putGiveawayTimer(long guildId, StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
        Giveaway.GiveawayTimerStorage giveawayTimerStorage = new Giveaway.GiveawayTimerStorage(stopGiveawayByTimer, timer);
        giveawayTimer.put(guildId, giveawayTimerStorage);
    }

    public void removeGuildFromGiveaway(long guildId) {
        giveawayMap.remove(guildId);
    }

    public void removeGiveawayTimer(long guildId) {
        giveawayTimer.remove(guildId);
    }
}