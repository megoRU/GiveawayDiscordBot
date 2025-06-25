package main.core.events;

import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class ReactionEvent {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionEvent.class.getName());

    public static final String TADA = "\uD83C\uDF89";
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void reaction(@NotNull MessageReactionAddEvent event, UpdateController updateController) {
        try {
            User user = event.retrieveUser().complete();
            Member member = event.getMember();

            if (member == null || user.isBot()) return;

            String emoji = event.getEmoji().getName();
            long messageId = event.getMessageIdLong();
            long guildIdLong = event.getGuild().getIdLong();
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Giveaway giveaway = instance.getGiveaway(messageId);

            if (giveaway != null) {
                GiveawayData giveawayData = giveaway.getGiveawayData();
                if (giveawayData.participantContains(user.getIdLong())) return;
                if (emoji.equals(TADA)) {
                    //Проверяем event id message с Giveaway message id
                    long messageIdWithReaction = giveawayData.getMessageId();

                    if (messageId != messageIdWithReaction) return;
                    Long roleId = giveawayData.getRoleId(); // null

                    if (roleId != null) {
                        Role roleById = event.getGuild().getRoleById(roleId);
                        boolean isForSpecificRole = giveawayData.isForSpecificRole();

                        if (isForSpecificRole && !event.getMember().getRoles().contains(roleById)) {
                            String url = GiveawayUtils.getDiscordUrlMessage(guildIdLong, event.getGuildChannel().getIdLong(), messageId);
                            LOGGER.info("Нажал на эмодзи, но у него нет доступа к розыгрышу: {}", user.getId());
                            //Получаем ссылку на Giveaway

                            String buttonGiveawayNotAccess = String.format(jsonParsers.getLocale("button_giveaway_not_access", event.getGuild().getIdLong()), url);
                            EmbedBuilder embedBuilder = new EmbedBuilder();
                            embedBuilder.setColor(Color.RED);
                            embedBuilder.setDescription(buttonGiveawayNotAccess);

                            updateController.setView(event.getJDA(), user.getId(), embedBuilder.build());
                            return;
                        }
                    }
                    giveaway.addUser(user);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}