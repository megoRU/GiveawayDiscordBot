package messagesevents;

import giveaway.ReactionsButton;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

import java.util.ArrayList;
import java.util.List;

public class MessageInfoHelp extends ListenerAdapter implements SenderMessage {

    private static final String HELP = "!help";
    private static final String PREFIX = "!";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final List<Button> buttons = new ArrayList<>();

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (!event.getGuild().getSelfMember().hasPermission(event.getMessage().getTextChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentDisplay().trim();

        if (message.equals("")) return;

        String prefix = HELP;
        String p = PREFIX;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "help";
            p = BotStart.getMapPrefix().get(event.getGuild().getId());
        }


        if (message.equals(prefix)) {
            buildMessage(
                    p,
                    event.getChannel(),
                    event.getAuthor().getAvatarUrl(),
                    event.getGuild().getId(),
                    event.getAuthor().getName());
        }

    }

    public void buildMessage(String p, TextChannel textChannel, String avatarUrl, String guildIdLong, String name) {

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
                        jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", guildIdLong) +
                        jsonParsers.getLocale("messages_events_Vote_For_This_Bot", guildIdLong), false);

        info.addField(
                jsonParsers.getLocale("messages_events_Bot_Creator", guildIdLong),
                jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", guildIdLong), false);

        info.addField(
                jsonParsers.getLocale("messages_events_Support", guildIdLong),
                jsonParsers.getLocale("messages_events_Support_Url_Discord", guildIdLong), false);

        buttons.add(Button.success(guildIdLong + ":" + ReactionsButton.BUTTON_EXAMPLES, jsonParsers.getLocale("button_Examples", guildIdLong)));

        sendMessage(info.build(), textChannel, buttons);
    }
}