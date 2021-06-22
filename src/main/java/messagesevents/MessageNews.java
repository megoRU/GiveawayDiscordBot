package messagesevents;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class MessageNews extends ListenerAdapter {

    private static final String NEWS_DISABLE = "!news disable";
    private static final String NEWS_ENABLED = "!news enable";
    private static final String NEWS_CHANNEL = "!news <#[0-9]+>";
    private static final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;
        if (event.getMember() == null) return;
        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) return;

        String message = event.getMessage().getContentRaw().toLowerCase().trim();
        String prefix_NEWS_DISABLE = NEWS_DISABLE;
        String prefix_NEWS_ENABLED = NEWS_ENABLED;
        String prefix_NEWS_CHANNEL = NEWS_CHANNEL;

        if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix_NEWS_DISABLE = BotStart.getMapPrefix().get(event.getGuild().getId()) + "news disable";
            prefix_NEWS_ENABLED = BotStart.getMapPrefix().get(event.getGuild().getId()) + "news enable";
            prefix_NEWS_CHANNEL = BotStart.getMapPrefix().get(event.getGuild().getId()) + "news <#[0-9]+>";
        }

        if ((message.equals(prefix_NEWS_DISABLE)
                || message.equals(prefix_NEWS_ENABLED)
                || message.equals(prefix_NEWS_CHANNEL))
                && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("language_change_Not_Admin", event.getGuild().getId()))
                    .queue();
            return;
        }


        if (prefix_NEWS_CHANNEL.matches(prefix_NEWS_CHANNEL)) {

            return;
        }

        if (prefix_NEWS_CHANNEL.matches(prefix_NEWS_DISABLE)) {

            return;
        }

        if (prefix_NEWS_CHANNEL.matches(prefix_NEWS_ENABLED)) {

        }


    }


}
