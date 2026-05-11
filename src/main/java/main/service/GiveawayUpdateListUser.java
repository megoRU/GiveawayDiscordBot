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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                    if (guildById != null) {
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
                                        if (roleGiveaway != null && guildById != null) {
                                            List<Long> userIds = users.stream()
                                                    .filter(user -> !user.isBot())
                                                    .filter(user -> !giveawayData.participantContains(user.getIdLong()))
                                                    .map(User::getIdLong)
                                                    .toList();
                                            List<ParticipantDTO> participantDTOList = new ArrayList<>();

                                            for (Long userId : userIds) {
                                                try {
                                                    Member member = guildById.retrieveMemberById(userId).complete();
                                                    if (member != null && member.getRoles().contains(roleGiveaway)) {
                                                        participantDTOList.add(new ParticipantDTO(member.getIdLong(), member.getUser().getName()));
                                                    }
                                                } catch (Exception e) {
                                                    LOGGER.error("Error retrieving member " + userId + ": " + e.getMessage());
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
                    if (e.getMessage() != null &&
                            e.getMessage().contains("10008: Unknown Message") ||
                            e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                        LOGGER.info("GiveawayUpdateList: {} удаляем", e.getMessage());
                        giveawayRepositoryService.deleteGiveaway(messageId);
                        instance.removeGiveaway(messageId);
                    } else {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
    }
}