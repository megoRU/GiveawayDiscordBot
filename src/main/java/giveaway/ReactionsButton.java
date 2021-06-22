package giveaway;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ClientType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.Statcord;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ReactionsButton extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private static final Logger LOGGER = Logger.getLogger(ReactionsButton.class.getName());
    public static final String emojiPresent = "🎁";
    public static final String emojiStopOne = "1️⃣";
    public static final String emojiStopTwo = "2️⃣";
    public static final String emojiStopThree = "3️⃣";

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {

        if (event.getButton() == null) return;

        if (event.getGuild() == null || event.getMember() == null) return;

        if (event.getUser().isBot()) return;

        try {
            if (GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().get(event.getGuild().getIdLong()) == null) {
                return;
            }

            if (!GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().get(event.getGuild().getIdLong()).equals(event.getMessageId())) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_WRITE)) {
            return;
        }

        if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.getChannel().sendMessage(jsonParsers
                    .getLocale("reactions_bot_dont_have_permissions", event.getGuild().getId())
                    .replaceAll("\\{0}", event.getGuild().getSelfMember().getUser().getName()))
                    .queue();
            return;
        }
        try {

            LOGGER.info(
                    "\nGuild id: " + event.getGuild().getId() + "" +
                            "\nUser id: " + event.getUser().getId() + "" +
                            "\nButton pressed: " + event.getButton().getId());

            boolean isUserAdmin = event.getMember().hasPermission(event.getGuildChannel(), Permission.ADMINISTRATOR);
            boolean isUserCanManageServer = event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE);

            long guild = event.getGuild().getIdLong();
            //if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiPresent)
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + emojiPresent)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                    .getListUsersHash(event.getUser().getId()) == null) {
                event.deferEdit().queue();

                GiveawayRegistry.getInstance()
                        .getActiveGiveaways()
                        .get(event.getGuild().getIdLong())
                        .addUserToPoll(event.getMember().getUser());
                Statcord.commandPost("gift", event.getUser().getId());
                return;
            }

            if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiPresent)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                    .getListUsersHash(event.getUser().getId()) != null) {
                event.deferEdit().queue();
                return;
            }

            if (GiveawayRegistry.getInstance().hasGift(guild) && (isUserCanManageServer || isUserAdmin)) {

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiStopOne)) {

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(event.getGuild().getIdLong())
                            .stopGift(event.getGuild().getIdLong(), 1);

                    Statcord.commandPost("gift stop", event.getUser().getId());
                    return;
                }

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiStopTwo)) {
                    event.deferEdit().queue();
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(event.getGuild().getIdLong())
                            .stopGift(event.getGuild().getIdLong(), 2);
                    Statcord.commandPost("gift stop 2", event.getUser().getId());
                    return;
                }

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiStopThree)) {
                    event.deferEdit().queue();
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(event.getGuild().getIdLong())
                            .stopGift(event.getGuild().getIdLong(), 3);
                    Statcord.commandPost("gift stop 3", event.getUser().getId());
                }
            } else {
                event.deferEdit().queue();
                event.getHook().sendMessage(jsonParsers
                        .getLocale("message_gift_Not_Admin", event.getGuild().getId())).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
