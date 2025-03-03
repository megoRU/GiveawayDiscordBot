package main.core.events;

import main.jsonparser.JSONParsers;
import main.service.SlashService;
import net.dv8tion.jda.api.EmbedBuilder;
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
        String helpEdit = jsonParsers.getLocale("help_edit", guildId);
        String helpEndMessage = jsonParsers.getLocale("help_end_message", guildId);

        Long check = SlashService.getCommandId("check");
        Long start = SlashService.getCommandId("start");
        Long stop = SlashService.getCommandId("stop");
        Long scheduling = SlashService.getCommandId("scheduling");
        Long cancel = SlashService.getCommandId("cancel");
        Long reroll = SlashService.getCommandId("reroll");
        Long predefined = SlashService.getCommandId("predefined");
        Long list = SlashService.getCommandId("list");
        Long settings = SlashService.getCommandId("settings");
        Long participants = SlashService.getCommandId("participants");
        Long edit = SlashService.getCommandId("edit");
        Long endMessage = SlashService.getCommandId("endmessage");

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(Color.GREEN);
        info.setTitle("Giveaway");
        info.addField("Slash Commands",
                String.format("""
                                </start:%s> - %s
                                </stop:%s> - %s
                                </scheduling:%s> - %s
                                </cancel:%s> - %s
                                </reroll:%s> - %s
                                </predefined:%s> - %s
                                </list:%s> - %s
                                </settings:%s> - %s
                                </participants:%s> - %s
                                </check:%s> - %s
                                </edit:%s> - %s
                                </endmessage:%s> - %s
                                """,
                        start, helpStart,
                        stop, helpStop,
                        scheduling, helpScheduling,
                        cancel, helpCancel,
                        reroll, helpReroll,
                        predefined, helpPredefined,
                        list, helpList,
                        settings, helpLanguage,
                        participants, helpParticipants,
                        check, helpPermissions,
                        edit, helpEdit,
                        endMessage, helpEndMessage), false);
        String messagesEventsLinks = jsonParsers.getLocale("messages_events_links", guildId);
        String messagesEventsSite = jsonParsers.getLocale("messages_events_site", guildId);
        String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_add_me_to_other_guilds", guildId);

        info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds, false);

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

        event.replyEmbeds(info.build()).setEphemeral(true).addActionRow(buttons).queue();
    }
}