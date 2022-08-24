package main.giveaway.impl;

public interface URLS {

    static String getDiscordUrlMessage(final long guildIdLong, final long textChannelId, final long messageIdLong) {
        return "https://discord.com/channels/" + guildIdLong + "/" + textChannelId + "/" + messageIdLong;
    }
}
