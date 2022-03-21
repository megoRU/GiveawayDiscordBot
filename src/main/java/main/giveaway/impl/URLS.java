package main.giveaway.impl;

public interface URLS {

    String GET_PARTICIPANTS = "http://193.163.203.77:8085/api/get-participants";
    String WINNERS = "http://193.163.203.77:8085/api/winners";
    String SAVE_PARTICIPANTS = "http://193.163.203.77:8085/api/participants";

    static String getDiscordUrlMessage(final String guildIdLong, final String textChannelId, final String messageIdLong) {
        return "https://discord.com/channels/" + guildIdLong + "/" + textChannelId + "/" + messageIdLong;
    }
}
