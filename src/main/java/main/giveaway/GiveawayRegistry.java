package main.giveaway;

import main.jsonparser.JSONParsers;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class GiveawayRegistry {

    //Возвращает GiveawayData по long id
    private static final Map<Long, Gift.GiveawayData> giveawayDataMap = new HashMap<>();
    private static final Map<Long, Timer> giveawayTimer = new HashMap<>();
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

    public Timer getGiveawayTimer(long guildId) {
        return giveawayTimer.get(guildId);
    }

    public static Map<Long, Gift.GiveawayData> getGiveawayDataMap() {
        return giveawayDataMap;
    }

    public String getIdUserWhoCreateGiveaway(long guildId) {
        return giveawayDataMap.get(guildId).getIdUserWhoCreateGiveaway();
    }

    public void putIdUserWhoCreateGiveaway(long guildId, String userId) {
        giveawayDataMap.get(guildId).setIdUserWhoCreateGiveaway(userId);
    }

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

    public Timestamp getEndGiveawayDate(long guildId) {
        return giveawayDataMap.get(guildId).getEndGiveawayDate();
    }

    public String getCountWinners(long guildId) {
        return giveawayDataMap.get(guildId).getCountWinners();
    }

    public String getMessageId(long guildId) {
        return giveawayDataMap.get(guildId).getMessageId();
    }

    public String getTitle(long guildId) {
        return giveawayDataMap.get(guildId).getTitle();
    }

    public void putEndGiveawayDate(long guildId, Timestamp timestamp) {
        giveawayDataMap.get(guildId).setEndGiveawayDate(timestamp);
    }

    public void putGiveawayTimer(long guildId, Timer timer) {
        giveawayTimer.put(guildId, timer);
    }

    public void putMessageId(long guildId, String messageId) {
        giveawayDataMap.get(guildId).setMessageId(messageId);
    }

    public void putChannelId(long guildId, String channelId) {
        giveawayDataMap.get(guildId).setChannelId(channelId);
    }

    public void putTitle(long guildId, String title) {
        giveawayDataMap.get(guildId).setTitle(title);
    }

    public void putGift(long guildId, Gift gift) {
        giveawayDataMap.put(guildId, gift.new GiveawayData());
    }

    public void putCountWinners(long guildId, String countWinners) {
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
        return jsonParsers.getLocale("giveaway_registry_Error", String.valueOf(guildId));
    }
}