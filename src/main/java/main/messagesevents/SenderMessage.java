package main.messagesevents;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;

public interface SenderMessage {

    static void sendMessage(MessageEmbed embedBuilder, Long guildId, Long textChannel) {
        try {
            Guild guildById = BotStartConfig.jda.getGuildById(guildId);
            if (guildById != null) {
                TextChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .sendMessageEmbeds(embedBuilder)
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sendMessage(MessageEmbed embedBuilder, Long guildId, Long textChannel, List<Button> buttons) {
        try {
            Guild guildById = BotStartConfig.jda.getGuildById(guildId);
            if (guildById != null) {
                TextChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .sendMessageEmbeds(embedBuilder)
                            .setActionRow(buttons)
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}