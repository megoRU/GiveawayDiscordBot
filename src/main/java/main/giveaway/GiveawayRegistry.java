package main.giveaway;

import main.jsonparser.JSONParsers;
import main.threads.StopGiveawayByTimer;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, Gift> giveawayDataMap = new HashMap<>();
    private static final Map<Long, Gift.GiveawayTimerStorage> giveawayTimer = new HashMap<>();
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

    @Nullable
    public Gift.GiveawayTimerStorage getGiveawayTimer(long guildId) {
        return giveawayTimer.get(guildId);
    }

    public static Collection<Gift> getAllGift() {
        return giveawayDataMap.values();
    }

    public long getIdUserWhoCreateGiveaway(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getCreatedUserId();
    }

    public void putIdUserWhoCreateGiveaway(long guildId, long userId) {
        giveawayDataMap.get(guildId).getGiveawayData().setCreatedUserId(userId);
    }

    @Nullable
    public String getUrlImage(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getUrlImage();
    }

    public void putUrlImage(long guildId, String urlImage) {
        giveawayDataMap.get(guildId).getGiveawayData().setUrlImage(urlImage);
    }

    public boolean getIsForSpecificRole(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getIsForSpecificRole();
    }

    public void putIsForSpecificRole(long guildId, boolean is_for_specific_role) {
        giveawayDataMap.get(guildId).getGiveawayData().setIsForSpecificRole(is_for_specific_role);
    }

    @Nullable
    public Long getRoleId(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getRoleId();
    }

    public void putRoleId(long guildId, Long roleIdLong) {
        giveawayDataMap.get(guildId).getGiveawayData().setRoleId(roleIdLong);
    }

    public Gift getGift(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getGift();
    }

    public boolean hasGift(long guildId) {
        return giveawayDataMap.containsKey(guildId);
    }

    @Nullable
    public Timestamp getEndGiveawayDate(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getEndGiveawayDate();
    }

    public int getCountWinners(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getCountWinners();
    }

    public long getMessageId(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getMessageId();
    }

    public String getTitle(long guildId) {
        return giveawayDataMap.get(guildId).getGiveawayData().getTitle();
    }

    public void putEndGiveawayDate(long guildId, Timestamp timestamp) {
        giveawayDataMap.get(guildId).getGiveawayData().setEndGiveawayDate(timestamp);
    }

    public void cancelGiveawayTimer(long guildId) {
        Gift.GiveawayTimerStorage giveawayTimerStorage = getGiveawayTimer(guildId);
        if (giveawayTimerStorage != null) {
            giveawayTimerStorage.getStopGiveawayByTimer().cancel();
            giveawayTimerStorage.getTimer().cancel();
        }
    }

    public void putGiveawayTimer(long guildId, StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
        Gift.GiveawayTimerStorage giveawayTimerStorage = new Gift.GiveawayTimerStorage(stopGiveawayByTimer, timer);
        giveawayTimer.put(guildId, giveawayTimerStorage);
    }

    public void putMessageId(long guildId, long messageId) {
        giveawayDataMap.get(guildId).getGiveawayData().setMessageId(messageId);
    }

    public void putChannelId(long guildId, long channelId) {
        giveawayDataMap.get(guildId).getGiveawayData().setChannelId(channelId);
    }

    public void putTitle(long guildId, String title) {
        giveawayDataMap.get(guildId).getGiveawayData().setTitle(title);
    }

    public void putGift(long guildId, Gift gift) {
        giveawayDataMap.put(guildId, gift);
    }

    public void putCountWinners(long guildId, int countWinners) {
        giveawayDataMap.get(guildId).getGiveawayData().setCountWinners(countWinners);
    }

    public void removeGuildFromGiveaway(long guildId) {
        giveawayDataMap.remove(guildId);
    }

    public void removeGiveawayTimer(long guildId) {
        giveawayTimer.remove(guildId);
    }

    //TODO: Удалять из Базы данных нужно ещё
    public String removeGiftExceptions(long guildId) {
        giveawayDataMap.remove(guildId);
        return jsonParsers.getLocale("giveaway_registry_error", String.valueOf(guildId));
    }
}