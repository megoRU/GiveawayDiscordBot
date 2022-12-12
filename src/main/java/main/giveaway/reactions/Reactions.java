package main.giveaway.reactions;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.logging.Logger;

import static main.giveaway.impl.URLS.getDiscordUrlMessage;

public class Reactions extends ListenerAdapter implements SenderMessage {

    public static final String TADA = "\uD83C\uDF89";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static Logger LOGGER = Logger.getLogger(Reactions.class.getName());

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        try {
            User user = event.retrieveUser().complete();
            Member member = event.getMember();

            if (member == null || user.isBot()) return;

            String emoji = event.getEmoji().getName();
            long guildIdLong = event.getGuild().getIdLong();
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Giveaway giveaway = instance.getGiveaway(guildIdLong);

            if (giveaway != null) {
                if (emoji.equals(TADA)) {
                    //Проверяем event id message с Giveaway message id
                    long messageIdWithReactionCurrent = event.getMessageIdLong();
                    long messageIdWithReaction = giveaway.getMessageId();

                    if (messageIdWithReactionCurrent != messageIdWithReaction) return;
                    Long roleId = giveaway.getRoleId(); // null -> 0

                    if (roleId != null && roleId != 0L) {
                        Role roleById = event.getGuild().getRoleById(roleId);
                        boolean isForSpecificRole = giveaway.isForSpecificRole();

                        if (isForSpecificRole && !event.getMember().getRoles().contains(roleById)) {
                            String url = getDiscordUrlMessage(guildIdLong, event.getGuildChannel().getIdLong(), messageIdWithReactionCurrent);
                            LOGGER.info(String.format("\nНажал на эмодзи, но у него нет доступа к розыгрышу: %s", user.getId()));
                            //Получаем ссылку на Giveaway

                            String buttonGiveawayNotAccess = String.format(jsonParsers.getLocale("button_giveaway_not_access", event.getGuild().getId()), url);
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setColor(Color.RED);
                            embedBuilder.setDescription(buttonGiveawayNotAccess);

                            SenderMessage.sendPrivateMessage(event.getJDA(), user.getId(), embedBuilder.build());
                            return;
                        }
                    }

                    if (!giveaway.hasUserInGiveaway(user.getId())) {
                        LOGGER.info(String.format("\nНовый участник: %s\nСервер: %s", user.getId(), event.getGuild().getId()));
                        giveaway.addUser(user);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
