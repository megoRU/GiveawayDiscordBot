package main.core.events;

import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.Settings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class HelpCommand {

    private final static JSONParsers jsonParsers = new JSONParsers();

    public void help(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        String helpStart = jsonParsers.getLocale("help_start", guildId);
        String helpStop = jsonParsers.getLocale("help_stop", guildId);
        String helpScheduling = jsonParsers.getLocale("help_scheduling", guildId);
        String helpCancel = jsonParsers.getLocale("help_cancel", guildId);
        String helpReroll = jsonParsers.getLocale("help_reroll", guildId);
        String helpPredefined = jsonParsers.getLocale("help_predefined", guildId);
        String helpList = jsonParsers.getLocale("help_list", guildId);
        String helpLanguage = jsonParsers.getLocale("help_language", guildId);
        String helpParticipants = jsonParsers.getLocale("help_participants", guildId);
        String helpPermissions = jsonParsers.getLocale("help_permissions", guildId);
        String helpChange = jsonParsers.getLocale("help_change", guildId);

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(Color.GREEN);
        info.setTitle("Giveaway");
        info.addField("Slash Commands",
                String.format("""
                        </start:941286272390037535> - %s
                        </stop:941286272390037536> - %s
                        </scheduling:1102283573349851166> - %s
                        </cancel:1102283573349851167> - %s
                        </reroll:957624805446799452> - %s
                        </predefined:1049647289779630080> - %s
                        </list:941286272390037538> - %s
                        </settings:1204911821056905277> - %s
                        </participants:952572018077892638> - %s
                        </check-bot-permission:1009065886335914054> - %s
                        </change:1027901550456225842> - %s
                        """,
                        helpStart,
                        helpStop,
                        helpScheduling,
                        helpCancel,
                        helpReroll,
                        helpPredefined,
                        helpList,
                        helpLanguage,
                        helpParticipants,
                        helpPermissions,
                        helpChange), false);
        String messagesEventsLinks = jsonParsers.getLocale("messages_events_links", guildId);
        String messagesEventsSite = jsonParsers.getLocale("messages_events_site", guildId);
        String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_add_me_to_other_guilds", guildId);

        info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds, false);

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

        Settings settings = BotStart.getMapLanguages().get(guildId);

        if (settings != null) {
            if (settings.getLanguage().equals("eng")) {
                buttons.add(Button.secondary(guildId + ":" + ButtonChangeLanguage.CHANGE_LANGUAGE, "Сменить язык ")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            } else {
                buttons.add(Button.secondary(guildId + ":" + ButtonChangeLanguage.CHANGE_LANGUAGE, "Change language ")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
            }
        } else {
            buttons.add(Button.secondary(guildId + ":" + ButtonChangeLanguage.CHANGE_LANGUAGE, "Сменить язык ")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        }

        event.replyEmbeds(info.build()).setEphemeral(true).addActionRow(buttons).queue();
    }
}