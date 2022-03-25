package main.giveaway.reactions;

import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.logging.Logger;

import static main.giveaway.impl.URLS.getDiscordUrlMessage;

public class Reactions extends ListenerAdapter {

    public static final String TADA = "\uD83C\uDF89";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static Logger LOGGER = Logger.getLogger(Reactions.class.getName());

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        try {
            User user = event.retrieveUser().complete();
            if (user == null || user.isBot()) return;
            if (!event.getReactionEmote().isEmoji()) return;

            String emoji = event.getReactionEmote().getEmoji();
            long guildIdLong = event.getGuild().getIdLong();

            if (GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                if (emoji.equals(TADA)) {
                    //Проверяем event id message с Giveaway message id
                    String messageIdWithReactionCurrent = event.getMessageId();
                    String messageIdWithReaction = GiveawayRegistry.getInstance().getMessageId(guildIdLong);

                    if (!messageIdWithReactionCurrent.equals(messageIdWithReaction)) return;

                    Gift gift = GiveawayRegistry.getInstance().getGift(guildIdLong);
                    String roleId = String.valueOf(GiveawayRegistry.getInstance().getRoleId(guildIdLong));

                    if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildIdLong) && !event.getMember().getRoles().toString().contains(roleId)) {
                        LOGGER.info("\nНажал на эмодзи, но у него нет доступа к розыгрышу: " + user.getId());
                        //Получаем ссылку до сообщения
                        String url = getDiscordUrlMessage(event.getGuild().getId(), event.getTextChannel().getId(), event.getReaction().getMessageId());

                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setColor(Color.RED);
                        embedBuilder.setDescription(jsonParsers.getLocale("button_giveaway_not_access", event.getGuild().getId()).replaceAll("\\{0}", url));

                        sendPrivateMessage(event.getJDA(), user.getId(), embedBuilder.build());
                        return;
                    }

                    if (!gift.getListUsersHash(user.getId())) {
                        LOGGER.info("\nНовый участник: " + user.getId() + "\nСервер: " + event.getGuild().getId());
                        GiveawayRegistry.getInstance().getGift(guildIdLong).addUserToPoll(user);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPrivateMessage(JDA jda, String userId, MessageEmbed messageEmbed) {
        RestAction<User> action = jda.retrieveUserById(userId);
        action.submit()
                .thenCompose((user) -> user.openPrivateChannel().submit())
                .thenCompose((channel) -> channel.sendMessageEmbeds(messageEmbed).submit())
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
                            System.out.println("50007: Cannot send messages to this user");
                        }
                    }
                });
    }
}
