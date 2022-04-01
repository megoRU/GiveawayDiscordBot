package main.messagesevents;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MessageInfoHelp extends ListenerAdapter {

    private static final String HELP = "!help";

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

        if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "help";
        }

        if (message.equals(prefix)) {
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
            buttons.add(Button.link("https://discord.com/oauth2/authorize?client_id=808277484524011531&permissions=2147511360&scope=bot+applications.commands", "Add Slash Commands"));

            event.getChannel()
                    .sendMessage("Deprecated. Use `/Slash Commands`")
                    .setActionRow(buttons)
                    .queue();
        }
    }
}