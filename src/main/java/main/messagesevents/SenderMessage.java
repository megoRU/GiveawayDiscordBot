package main.messagesevents;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SenderMessage {

    default void sendMessage(MessageEmbed embedBuilder, TextChannel textChannel, List<Button> buttons) {
        try {
            textChannel.sendMessageEmbeds(embedBuilder).setActionRow(buttons).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void sendMessage(MessageEmbed embedBuilder, @NotNull SlashCommandEvent event, List<Button> buttons) {
        try {
            event.replyEmbeds(embedBuilder).addActionRow(buttons).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}