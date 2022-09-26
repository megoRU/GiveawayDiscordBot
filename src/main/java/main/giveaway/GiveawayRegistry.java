package main.giveaway;

import main.jsonparser.JSONParsers;
import main.threads.StopGiveawayByTimer;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, Gift.GiveawayData> giveawayDataMap = new HashMap<>();
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

    public static Map<Long, Gift.GiveawayData> getGiveawayDataMap() {
        return giveawayDataMap;
    }

    public long getIdUserWhoCreateGiveaway(long guildId) {
        return giveawayDataMap.get(guildId).getIdUserWhoCreateGiveaway();
    }

    public void putIdUserWhoCreateGiveaway(long guildId, long userId) {
        giveawayDataMap.get(guildId).setIdUserWhoCreateGiveaway(userId);
    }

    @Nullable
    public String getUrlImage(long guildId) {
        return giveawayDataMap.get(guildId).getUrlImage();
    }

    public void putUrlImage(long guildId, String urlImage) {
        giveawayDataMap.get(guildId).setUrlImage(urlImage);
    }

    public boolean getIsForSpecificRole(long guildId) {
        return giveawayDataMap.get(guildId).getIsForSpecificRole();
    }

    public void putIsForSpecificRole(long guildId, boolean is_for_specific_role) {
        giveawayDataMap.get(guildId).setIsForSpecificRole(is_for_specific_role);
    }

    @Nullable
    public Long getRoleId(long guildId) {
        return giveawayDataMap.get(guildId).getRoleId();
    }

    public void putRoleId(long guildId, Long roleIdLong) {
        giveawayDataMap.get(guildId).setRoleId(roleIdLong);
    }

    public Gift getGift(long guildId) {
        return giveawayDataMap.get(guildId).getGift();
    }

    public boolean hasGift(long guildId) {
        return giveawayDataMap.containsKey(guildId);
    }

    @Nullable
    public Timestamp getEndGiveawayDate(long guildId) {
        return giveawayDataMap.get(guildId).getEndGiveawayDate();
    }

    public int getCountWinners(long guildId) {
        return giveawayDataMap.get(guildId).getCountWinners();
    }

    public long getMessageId(long guildId) {
        return giveawayDataMap.get(guildId).getMessageId();
    }

    public String getTitle(long guildId) {
        return giveawayDataMap.get(guildId).getTitle();
    }

    public void putEndGiveawayDate(long guildId, Timestamp timestamp) {
        giveawayDataMap.get(guildId).setEndGiveawayDate(timestamp);
    }

    public void putGiveawayTimer(long guildId, StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
        Gift.GiveawayTimerStorage giveawayTimerStorage = new Gift.GiveawayTimerStorage(stopGiveawayByTimer, timer);
        giveawayTimer.put(guildId, giveawayTimerStorage);
    }

    public void putMessageId(long guildId, long messageId) {
        giveawayDataMap.get(guildId).setMessageId(messageId);
    }

    public void putChannelId(long guildId, long channelId) {
        giveawayDataMap.get(guildId).setChannelId(channelId);
    }

    public void putTitle(long guildId, String title) {
        giveawayDataMap.get(guildId).setTitle(title);
    }

    public void putGift(long guildId, Gift gift) {
        giveawayDataMap.put(guildId, gift.new GiveawayData());
    }

    public void putCountWinners(long guildId, int countWinners) {
        giveawayDataMap.get(guildId).setCountWinners(countWinners);
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