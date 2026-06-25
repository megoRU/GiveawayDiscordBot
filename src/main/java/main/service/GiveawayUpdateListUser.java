package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.core.events.ReactionEvent;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.giveaway.ParticipantDTO;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class GiveawayUpdateListUser {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayUpdateListUser.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    public void updateGiveawayByGuild(@NotNull Giveaway giveaway) {
        GiveawayData giveawayData = giveaway.getGiveawayData();

        long guildId = giveaway.getGuildId();
        long channelId = giveaway.getTextChannelId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        long messageId = giveawayData.getMessageId();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        JDA jda = BotStart.getJda();

        if (jda != null) {
            if (instance.hasGiveaway(messageId)) {
                try {
                    Guild guildById = jda.getGuildById(guildId);

                    if (guildById == null) {
                        giveawayRepositoryService.deleteGiveaway(messageId);
                        instance.removeGiveaway(messageId);
                    } else {
                        TextChannel textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            Message message = textChannelById.retrieveMessageById(messageId).complete(true);
                            List<MessageReaction> reactions = message.getReactions()
                                    .stream()
                                    .filter(messageReaction -> messageReaction.getEmoji().getName().equals(ReactionEvent.TADA))
                                    .toList();

                            if (!reactions.isEmpty()) {
                                MessageReaction reaction = reactions.getFirst();
                                int count = reaction.getCount();
                                if (reaction.isSelf()) {
                                    count--;
                                }

                                if (count != giveawayData.getParticipantSize()) {
                                    List<User> users = new ArrayList<>();
                                    reaction.retrieveUsers().forEach(users::add);

                                    if (isForSpecificRole) {
                                        Role roleGiveaway = jda.getRoleById(giveawayData.getRoleId());
                                        if (roleGiveaway != null) {
                                            List<Long> userIds = users.stream()
                                                    .filter(user -> !user.isBot())
                                                    .map(User::getIdLong)
                                                    .filter(idLong -> !giveawayData.participantContains(idLong))
                                                    .toList();
                                            List<ParticipantDTO> participantDTOList = new ArrayList<>();

                                            for (Long userId : userIds) {
                                                try {
                                                    Member member = guildById.retrieveMemberById(userId).complete();
                                                    if (member != null && member.getRoles().contains(roleGiveaway)) {
                                                        participantDTOList.add(new ParticipantDTO(member.getIdLong(), member.getUser().getName()));
                                                    }
                                                } catch (Exception e) {
                                                    LOGGER.error("Error retrieving member {}: {}", userId, e.getMessage());
                                                }
                                            }
                                            if (instance.hasGiveaway(messageId) && !participantDTOList.isEmpty()) {
                                                giveaway.addUser(participantDTOList);
                                            }
                                        }
                                    } else {
                                        if (instance.hasGiveaway(messageId)) {
                                            List<ParticipantDTO> participantDTOList = users.stream()
                                                    .filter(user -> !user.isBot())
                                                    .filter(user -> !giveawayData.participantContains(user.getIdLong()))
                                                    .map(user -> new ParticipantDTO(user.getIdLong(), user.getName()))
                                                    .toList();

                                            if (!participantDTOList.isEmpty()) {
                                                giveaway.addUser(participantDTOList);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof ErrorResponseException ex) {
                        ErrorResponse error = ex.getErrorResponse();

                        if (error == ErrorResponse.UNKNOWN_MESSAGE ||
                                error == ErrorResponse.MISSING_ACCESS ||
                                error == ErrorResponse.MISSING_PERMISSIONS ||
                                error == ErrorResponse.UNKNOWN_CHANNEL) {

                            LOGGER.info("GiveawayUpdateList: {} удаляем", error);
                            giveawayRepositoryService.deleteGiveaway(messageId);
                            instance.removeGiveaway(messageId);
                            return;
                        }
                    }

                    LOGGER.warn("Не удалось обновить участников Giveaway, повторим позже", e);
                }
            }
        }
    }
}