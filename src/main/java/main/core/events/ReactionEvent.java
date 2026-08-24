package main.core.events;

import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

            GiveawayRepositoryService giveawayRepositoryService = updateController.getGiveawayRepositoryService();
            ActiveGiveaways activeGiveaways = giveawayRepositoryService.getGiveaway(messageId);

            if (activeGiveaways != null) {
                Set<Long> participantsList = activeGiveaways.getParticipants() != null ? activeGiveaways.getParticipants()
                        .stream()
                        .map(Participants::getUserId)
                        .collect(Collectors.toSet()) : Collections.emptySet();

                Giveaway giveaway = new Giveaway(
                        activeGiveaways.getGuildId(),
                        activeGiveaways.getChannelId(),
                        activeGiveaways.getCreatedUserId(),
                        activeGiveaways.isFinish(),
                        false,
                        activeGiveaways.getMessageId(),
                        activeGiveaways.getCountWinners(),
                        activeGiveaways.getRoleId(),
                        activeGiveaways.getIsForSpecificRole(),
                        activeGiveaways.getUrlImage(),
                        activeGiveaways.getTitle(),
                        activeGiveaways.getEndGiveawayDate(),
                        activeGiveaways.getMinParticipants() == null ? 1 : activeGiveaways.getMinParticipants(),
                        giveawayRepositoryService,
                        updateController
                );
                giveaway.setParticipantsList(participantsList);

                if (giveaway.participantContains(user.getIdLong())) return;
                if (emoji.equals(TADA)) {
                    long messageIdWithReaction = giveaway.getMessageId();

                    if (messageId != messageIdWithReaction) return;
                    Long roleId = giveaway.getRoleId();

                    if (roleId != null) {
                        Role roleById = event.getGuild().getRoleById(roleId);
                        boolean isForSpecificRole = giveaway.isForSpecificRole();

                        if (isForSpecificRole && roleById != null && !event.getMember().getRoles().contains(roleById)) {
                            String url = GiveawayUtils.getDiscordUrlMessage(guildIdLong, event.getGuildChannel().getIdLong(), messageId);
                            LOGGER.info("Нажал на эмодзи, но у него нет доступа к розыгрышу: {}", user.getId());

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