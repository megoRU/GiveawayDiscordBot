package main.core.events;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayMessageHandler;
import main.giveaway.GiveawayRegistry;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ReactionEvent {

    private final static Logger LOGGER = Logger.getLogger(ReactionEvent.class.getName());
    public static final String TADA = "\uD83C\uDF89";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final GiveawayMessageHandler giveawayMessageHandler;

    @Autowired
    public ReactionEvent(GiveawayMessageHandler giveawayMessageHandler) {
        this.giveawayMessageHandler = giveawayMessageHandler;
    }

    public void reaction(@NotNull MessageReactionAddEvent event) {
        try {
            User user = event.retrieveUser().complete();
            Member member = event.getMember();
            long guildId = event.getGuild().getIdLong();

            if (member == null || user.isBot()) return;

            String emoji = event.getEmoji().getName();
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            boolean hasGiveaway = instance.hasGiveaway(guildId);

            if (hasGiveaway) {
                Giveaway giveaway = instance.getGiveaway(guildId);
                if (giveaway != null) {
                    if (emoji.equals(TADA)) {
                        //Проверяем event id message с Giveaway message id
                        long messageIdWithReactionCurrent = event.getMessageIdLong();
                        long messageIdWithReaction = giveaway.getMessageId();

                        if (messageIdWithReactionCurrent != messageIdWithReaction) return;
                        Long roleId = giveaway.getRoleId(); // null -> 0
                        Long forbiddenRole = giveaway.getForbiddenRole();

                        if (forbiddenRole != null) {
                            Role guildRole = event.getGuild().getRoleById(forbiddenRole);
                            if (guildRole == null) return;
                            List<Long> memberRolesLost = member.getRoles().stream().map(Role::getIdLong).toList();

                            if (memberRolesLost.contains(guildRole.getIdLong())) {
                                userDontHaveRestrictions(event, guildId, user);
                                return;
                            }
                        }

                        if (roleId != null) {
                            Role role = event.getGuild().getRoleById(roleId);
                            if (role == null) return;
                            boolean isForSpecificRole = giveaway.isForSpecificRole();
                            List<Long> userRolesList = member.getRoles().stream().map(Role::getIdLong).toList();

                            if (isForSpecificRole && !userRolesList.contains(role.getIdLong())) {
                                userDontHaveRestrictions(event, guildId, user);
                                return;
                            }
                        }

                        giveaway.addUser(user);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void userDontHaveRestrictions(@NotNull MessageReactionAddEvent event, long guildId, User user) {
        long messageIdWithReactionCurrent = event.getMessageIdLong();
        String url = GiveawayUtils.getDiscordUrlMessage(guildId, event.getGuildChannel().getIdLong(), messageIdWithReactionCurrent);

        LOGGER.info(String.format("\nНажал на эмодзи, но у него нет доступа к розыгрышу: %s", user.getId()));
        //Получаем ссылку на Giveaway
        String buttonGiveawayNotAccess = String.format(jsonParsers.getLocale("button_giveaway_not_access", guildId), url);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setDescription(buttonGiveawayNotAccess);

        giveawayMessageHandler.sendMessage(event.getJDA(), user.getIdLong(), embedBuilder.build());
    }
}