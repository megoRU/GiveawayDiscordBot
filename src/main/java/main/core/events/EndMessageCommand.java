package main.core.events;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EndMessageCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        var text = event.getOption("text", OptionMapping::getAsString);
        var messageId = event.getOption("message-id", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (messageId != null && text != null && messageId.matches("[0-9]+")) {
            long id = Long.parseLong(messageId);
            try {
                event.getChannel().editMessageById(id, text).submit().get();
            } catch (Exception e) {
                String endMessageEditError = jsonParsers.getLocale("end_message_edit_error", guildId);
                event.reply(endMessageEditError).setEphemeral(true).queue();
                return;
            }
            String endMessageEdit = jsonParsers.getLocale("end_message_edit", guildId);
            event.reply(endMessageEdit).setEphemeral(true).queue();
        } else {
            String idMustBeANumber = jsonParsers.getLocale("id_must_be_a_number", guildId);
            event.reply(idMustBeANumber).setEphemeral(true).queue();
        }
    }
}