package main.giveaway;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageGift extends ListenerAdapter {

    private static final String GIFT_START = "!gift start";
    private static final String GIFT_START_WITHOUT_PREFIX = "gift start";
    private static final String GIFT_START_WITH_MINUTES = "gift start\\s\\d{1,2}[mмhчdд]$";
    private static final String GIFT_START_TITLE = "gift start\\s.{0,255}$";
    private static final String GIFT_START_TITLE_MINUTES_WITH_COUNT = "gift start\\s.{0,255}\\s\\d{1,2}[mмhчdд]\\s\\d{1,2}$";
    private static final String GIFT_START_TITLE_WITH_MINUTES = "gift start\\s.{0,255}\\s[0-9]{1,2}[mмhчdд]$";
    private static final String GIFT_START_COUNT_WITH_MINUTES = "gift start\\s\\d{1,2}[mмhчdд]\\s\\d{1,2}$";
    private static final String GIFT_STOP = "gift stop";
    private static final String GIFT_STOP_COUNT = "gift stop\\s\\d+";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.TEXT)) return;

        String message = event.getMessage().getContentRaw().toLowerCase().trim();

        if (event.getMember() == null || message.equals("")) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_SEND) ||
                !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        int length = message.length();
        String messageWithOutPrefix = message.substring(1, length);

        if (messageWithOutPrefix.matches(GIFT_START_TITLE)
                || messageWithOutPrefix.matches(GIFT_START_WITHOUT_PREFIX)
                || messageWithOutPrefix.matches(GIFT_STOP)
                || messageWithOutPrefix.matches(GIFT_START)
                || messageWithOutPrefix.matches(GIFT_STOP_COUNT)
                || messageWithOutPrefix.matches(GIFT_START_COUNT_WITH_MINUTES)
                || messageWithOutPrefix.matches(GIFT_START_WITH_MINUTES)
                || messageWithOutPrefix.matches(GIFT_START_TITLE_MINUTES_WITH_COUNT)
                || messageWithOutPrefix.matches(GIFT_START_TITLE_WITH_MINUTES)) {
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