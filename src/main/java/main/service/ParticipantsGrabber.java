package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.giveaway.GiveawayUtils;
import main.giveaway.Participant;
import main.model.entity.ActiveGiveaways;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ParticipantsGrabber {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantsGrabber.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    public Set<Participant> get(@NotNull ActiveGiveaways activeGiveaway) throws Exception {
        long guildId = activeGiveaway.getGuildId();
        long channelId = activeGiveaway.getChannelId();
        long messageId = activeGiveaway.getMessageId();

        JDA jda = BotStart.getJda();

        if (jda == null) {
            throw new Exception("JDA is NULL");
        }

        try {
            Guild guild = jda.getGuildById(guildId);

            if (guild == null) {
                giveawayRepositoryService.deleteGiveaway(messageId);
                return Collections.emptySet();
            }

            TextChannel textChannel = guild.getTextChannelById(channelId);

            if (textChannel == null) {
                giveawayRepositoryService.deleteGiveaway(messageId);
                return Collections.emptySet();
            }

            Message message = textChannel.retrieveMessageById(messageId).complete(true);

            List<MessageReaction> reactions = message.getReactions()
                    .stream()
                    .filter(reaction -> reaction.getEmoji().getName().equals(GiveawayUtils.TADA))
                    .toList();

            if (reactions.isEmpty()) {
                return Collections.emptySet();
            }

            Long roleId = activeGiveaway.getRoleId();
            Role roleGiveaway = roleId != null ? jda.getRoleById(roleId) : null;

            return reactions.getFirst()
                    .retrieveUsers()
                    .stream()
                    .filter(user -> !user.isBot())
                    .filter(user -> {
                        if (roleGiveaway == null || !activeGiveaway.getIsForSpecificRole()) {
                            return true;
                        }

                        try {
                            Member member = guild.retrieveMemberById(user.getId()).complete();
                            return member != null && member.getRoles().contains(roleGiveaway);
                        } catch (Exception e) {
                            LOGGER.error("Error retrieving member {}", user.getId(), e);
                            return false;
                        }
                    })
                    .map(user -> new Participant(user.getIdLong(), user.getName()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            if (e instanceof ErrorResponseException ex) {
                ErrorResponse error = ex.getErrorResponse();

                if (error == ErrorResponse.UNKNOWN_MESSAGE
                        || error == ErrorResponse.MISSING_ACCESS
                        || error == ErrorResponse.MISSING_PERMISSIONS
                        || error == ErrorResponse.UNKNOWN_CHANNEL) {

                    LOGGER.info("GiveawayUpdateList: {} удаляем", error);
                    giveawayRepositoryService.deleteGiveaway(messageId);
                }
            }
            LOGGER.warn("Не удалось обновить участников Giveaway, повторим позже", e);
            throw e;
        }
    }
}