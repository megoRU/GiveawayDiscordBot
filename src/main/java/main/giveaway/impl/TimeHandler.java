package main.giveaway.impl;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public interface TimeHandler {

    JSONParsers jsonParsers = new JSONParsers();

    static boolean get(@NotNull SlashCommandInteractionEvent event, String guildId, String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, Formats.FORMATTER);
        LocalDateTime now = Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
        if (localDateTime.isBefore(now)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", (guildId));
            String youWroteDate = jsonParsers.getLocale("you_wrote_date", (guildId));

            String format = String.format(youWroteDate,
                    localDateTime.toString().replace("T", " "),
                    now.toString().substring(0, 16).replace("T", " "));

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle(wrongDate);
            builder.setDescription(format);

            event.replyEmbeds(builder.build()).queue();
            return true;
        }
        return false;
    }
}
