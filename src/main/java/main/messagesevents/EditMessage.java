package main.messagesevents;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public interface EditMessage {

    static void edit(MessageEmbed embedBuilder, Long guildId, Long textChannel, Long messageId) {
        try {
            Guild guildById = BotStartConfig.getJda().getGuildById(guildId);
            if (guildById != null) {
                TextChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .editMessageEmbedsById(messageId, embedBuilder)
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
