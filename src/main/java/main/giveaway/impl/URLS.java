package main.giveaway.impl;

public interface URLS {

    static String getDiscordUrlMessage(final String guildIdLong, final String textChannelId, final String messageIdLong) {
        return "https://discord.com/channels/" + guildIdLong + "/" + textChannelId + "/" + messageIdLong;
    }
}
