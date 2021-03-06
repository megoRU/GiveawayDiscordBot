package giveaway;

import jsonparser.JSONParsers;
import messagesevents.MessageInfoHelp;
import messagesevents.SenderMessage;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import startbot.Statcord;

import java.util.Objects;
import java.util.logging.Logger;

public class ReactionsButton extends ListenerAdapter implements SenderMessage {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final static Logger LOGGER = Logger.getLogger(ReactionsButton.class.getName());
    public static final String PRESENT = "PRESENT";
    public static final String STOP_ONE = "STOP_ONE";
    public static final String STOP_TWO = "STOP_TWO";
    public static final String STOP_THREE = "STOP_THREE";
    public static final String BUTTON_EXAMPLES = "BUTTON_EXAMPLES";
    public static final String BUTTON_HELP = "BUTTON_HELP";

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {

        if (event.getButton() == null) return;

        if (event.getGuild() == null || event.getMember() == null) return;

        if (event.getUser().isBot()) return;

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_EXAMPLES)) {

            event.deferEdit().queue();
            event.getChannel().sendMessage(jsonParsers
                    .getLocale("message_gift_Not_Correct", event.getGuild().getId())
                    .replaceAll("\\{0}",
                            BotStart.getMapPrefix().get(event.getGuild().getId())
                                    == null
                                    ? "!"
                                    : BotStart.getMapPrefix().get(event.getGuild().getId())))
                    .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP,
                            jsonParsers.getLocale("button_Help", event.getGuild().getId())))
                    .queue();
            return;
        }

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_HELP)) {

            event.deferEdit().queue();
            MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
            messageInfoHelp.buildMessage(
                    BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStart.getMapPrefix().get(event.getGuild().getId()),
                    event.getTextChannel(),
                    event.getUser().getAvatarUrl(),
                    event.getGuild().getId(),
                    event.getUser().getName()
            );
            return;
        }


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
        try {

            LOGGER.info(
                    "\nGuild id: " + event.getGuild().getId() + "" +
                            "\nUser id: " + event.getUser().getId() + "" +
                            "\nButton pressed: " + event.getButton().getId());

            boolean isUserAdmin = event.getMember().hasPermission(event.getGuildChannel(), Permission.ADMINISTRATOR);
            boolean isUserCanManageServer = event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE);

            long guild = event.getGuild().getIdLong();
            //if (event.getButton().getId().equals(event.getGuild().getId() + ":" + emojiPresent)
            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + PRESENT)
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

            if (event.getButton().getId().equals(event.getGuild().getId() + ":" + PRESENT)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                    .getListUsersHash(event.getUser().getId()) != null) {
                event.deferEdit().queue();
                return;
            }

            if (GiveawayRegistry.getInstance().hasGift(guild) && (isUserCanManageServer || isUserAdmin)) {

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + STOP_ONE)) {

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(event.getGuild().getIdLong())
                            .stopGift(event.getGuild().getIdLong(), 1);

                    Statcord.commandPost("gift stop", event.getUser().getId());
                    return;
                }

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + STOP_TWO)) {
                    event.deferEdit().queue();
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(event.getGuild().getIdLong())
                            .stopGift(event.getGuild().getIdLong(), 2);
                    Statcord.commandPost("gift stop 2", event.getUser().getId());
                    return;
                }

                if (event.getButton().getId().equals(event.getGuild().getId() + ":" + STOP_THREE)) {
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
