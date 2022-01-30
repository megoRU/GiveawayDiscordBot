package main.giveaway;

import lombok.Getter;
import lombok.Setter;
import main.jsonparser.JSONParsers;

import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GiveawayRegistry {

    //Возвращает GiftData по long id
    private static final ConcurrentMap<Long, GiveawayData> giveawayDataMap = new ConcurrentHashMap<>();
    private static final JSONParsers jsonParsers = new JSONParsers();
    private static volatile GiveawayRegistry giveawayRegistry;

    private GiveawayRegistry() {
    }

    @Getter
    @Setter
    public static class GiveawayData {

        private Gift gift;
        private String idMessagesWithGiveawayButtons;
        private String channelId;
        private String messageId;
        private String countWinners;
        private String title;
        private Timestamp endGiveawayDate;

        public GiveawayData(Gift gift) {
            this.gift = gift;
        }

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

    public Gift getGift(long guildId) {
        return giveawayDataMap.get(guildId).getGift();
    }

    public GiveawayData getGiveawayDataMap(long guildId) {
        return giveawayDataMap.get(guildId);
    }

    public void removeGuildFromGiveaway(long guildId) {
        giveawayDataMap.remove(guildId);
    }


    public void putEndGiveawayDate(long guildId, Timestamp timestamp) {
        giveawayDataMap.get(guildId).setEndGiveawayDate(timestamp);
    }

    public void putMessageId(long guildId, String messageId) {
        giveawayDataMap.get(guildId).setMessageId(messageId);
    }

    public void putChannelId(long guildId, String channelId) {
        giveawayDataMap.get(guildId).setChannelId(channelId);
    }

    public void putIdMessagesWithGiveawayButtons(long guildId, String idMessageWithButton) {
        giveawayDataMap.get(guildId).setIdMessagesWithGiveawayButtons(idMessageWithButton);
    }

    public String getIdMessagesWithGiveawayButtons(long guildId) {
        return giveawayDataMap.get(guildId).getIdMessagesWithGiveawayButtons();
    }

    public Timestamp getEndGiveawayDate(long guildId) {
        return giveawayDataMap.get(guildId).getEndGiveawayDate();
    }

    public String getMessageId(long guildId) {
        return giveawayDataMap.get(guildId).getMessageId();
    }


    public String getTitle(long guildId) {
        return giveawayDataMap.get(guildId).getTitle();
    }

    public void putTitle(long guildId, String title) {
        giveawayDataMap.get(guildId).setTitle(title);
    }

    public void putGift(long guildId, Gift gift) {
        giveawayDataMap.put(guildId, new GiveawayData(gift));
    }

    public boolean hasGift(long guildId) {
        return giveawayDataMap.containsKey(guildId);
    }

    public String getCountWinners(long guildId) {
        return giveawayDataMap.get(guildId).getCountWinners();
    }

    public void putCountWinners(long guildId, String countWinners) {
        giveawayDataMap.get(guildId).setCountWinners(countWinners);
    }

    public void removeGift(long guildId) {
        giveawayDataMap.remove(guildId);
    }

    //TODO: Удалять из Базы данных нужно ещё
    public String removeGiftExceptions(long guildId) {
        giveawayDataMap.remove(guildId);
        return jsonParsers.getLocale("giveaway_registry_Error", String.valueOf(guildId));
    }
}