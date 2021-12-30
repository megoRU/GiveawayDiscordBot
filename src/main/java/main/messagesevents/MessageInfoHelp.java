package main.messagesevents;

import main.config.BotStartConfig;
import main.giveaway.ReactionsButton;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MessageInfoHelp extends ListenerAdapter implements SenderMessage {

    private static final String HELP = "!help";
    private static final String PREFIX = "!";
    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.TEXT)) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_SEND) ||
                !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        String message = event.getMessage().getContentDisplay().trim();

        if (event.getMember() == null || message.equals("")) return;

        String prefix = HELP;
        String p = PREFIX;

        if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "help";
            p = BotStartConfig.getMapPrefix().get(event.getGuild().getId());
        }


        if (message.equals(prefix)) {
            buildMessage(
                    p,
                    event.getTextChannel(),
                    event.getAuthor().getAvatarUrl(),
                    event.getGuild().getId(),
                    event.getAuthor().getName(),
                    null);
        }

    }

    public void buildMessage(String p, TextChannel textChannel, String avatarUrl, String guildIdLong, String name, SlashCommandEvent event) {

        if (!textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_SEND) ||
                !textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        String avatar = null;

        if (avatarUrl == null) {
            avatar = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
        }
        if (avatarUrl != null) {
            avatar = avatarUrl;
        }

        EmbedBuilder info = new EmbedBuilder();
        info.setColor(0xa224db);
        info.setAuthor(name, null, avatar);
        info.addField(jsonParsers.getLocale("messages_events_Prefix", guildIdLong),
                jsonParsers.getLocale("messages_events_Changes_Prefix", guildIdLong) +
                        jsonParsers.getLocale("messages_events_Reset_Prefix", guildIdLong), false);

        info.addField(jsonParsers.getLocale("messages_events_Language_Title", guildIdLong), "`"
                        + p + jsonParsers.getLocale("messages_events_Language", guildIdLong) + "`"
                        + p + jsonParsers.getLocale("messages_events_Language_Reset", guildIdLong)
                , false);

        info.addField("Giveaway:",
                jsonParsers.getLocale("messages_events_PS", guildIdLong)
                        + "`"
                        + p + jsonParsers.getLocale("messages_events_Start_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Start_Text_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Start_Text_Time_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Start_Text_Time_Count_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Start_Time_Count_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Stop_Giveaway", guildIdLong)
                        + p + jsonParsers.getLocale("messages_events_Stop_Number_Giveaway", guildIdLong), false);

        info.addField(jsonParsers.getLocale("messages_events_Links", guildIdLong),
                jsonParsers.getLocale("messages_events_Site", guildIdLong) +
                        jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", guildIdLong), false);

        List<Button> buttons = new ArrayList<>();

        buttons.add(Button.success(guildIdLong + ":" + ReactionsButton.BUTTON_EXAMPLES, jsonParsers.getLocale("button_Examples", guildIdLong)));
        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));


        if (BotStartConfig.getMapLanguages().get(guildIdLong) != null) {

            if (BotStartConfig.getMapLanguages().get(guildIdLong).equals("eng")) {

                buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                "Сменить язык ")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            } else {
                buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                "Change language ")
                        .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
            }
        } else {
            buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                            "Сменить язык ")
                    .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
        }

        if (event == null) {
            sendMessage(info.build(), textChannel, buttons);
        } else {
            sendMessage(info.build(), event, buttons);
        }
    }
}