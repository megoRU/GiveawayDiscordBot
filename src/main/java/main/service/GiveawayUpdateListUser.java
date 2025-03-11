package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.core.events.ReactionEvent;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GiveawayUpdateListUser {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayUpdateListUser.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final static GiveawayRegistry instance = GiveawayRegistry.getInstance();

    public void updateGiveawayByGuild(@NotNull Giveaway giveaway) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        long guildId = giveaway.getGuildId();
        long channelId = giveaway.getTextChannelId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        long messageId = giveawayData.getMessageId();

        JDA jda = BotStart.getJda();
        if (jda != null) {
            if (instance.hasGiveaway(messageId)) {
                Guild guildById = jda.getGuildById(guildId);
                if (guildById != null) {
                    TextChannel textChannelById = guildById.getTextChannelById(channelId);
                    if (textChannelById != null) {
                        textChannelById.retrieveMessageById(messageId)
                                .queue(message -> {
                                    List<MessageReaction> reactions = message.getReactions().stream()
                                            .filter(messageReaction -> messageReaction.getEmoji().getName().equals(ReactionEvent.TADA))
                                            .toList();

                                    if (!reactions.isEmpty() && reactions.getFirst().getCount() - 1 != giveawayData.getParticipantSize()) {
                                        for (MessageReaction reaction : reactions) {
                                            reaction.retrieveUsers()
                                                    .queue(users -> {
                                                        Map<String, User> userList = users.stream()
                                                                .filter(user -> !user.isBot())
                                                                .filter(user -> !giveawayData.participantContains(user.getId()))
                                                                .collect(Collectors.toMap(User::getId, user -> user));

                                                        if (isForSpecificRole) {
                                                            try {
                                                                Map<String, User> localUserMap = new HashMap<>(userList);
                                                                Role roleGiveaway = jda.getRoleById(giveawayData.getRoleId());
                                                                for (Map.Entry<String, User> entry : localUserMap.entrySet()) {
                                                                    Guild guild = jda.getGuildById(guildId);
                                                                    if (guild != null) {
                                                                        guild.retrieveMemberById(entry.getKey()).queue(member -> {
                                                                            if (member != null) {
                                                                                boolean contains = member.getRoles().contains(roleGiveaway);
                                                                                if (!contains) {
                                                                                    userList.remove(entry.getKey());
                                                                                }
                                                                            }
                                                                        }, throwable -> handleEditException(throwable, messageId));
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                LOGGER.error(e.getMessage(), e);
                                                            }
                                                        }

                                                        // Перебираем Users в реакциях
                                                        if (instance.hasGiveaway(messageId)) {
                                                            giveaway.addUser(userList.values().stream().toList());
                                                        }
                                                    });
                                        }
                                    }
                                }, throwable -> handleEditException(throwable, messageId));
                    }
                }
            }
        }
    }

    private void handleEditException(Throwable throwable, long messageId) {
        String message = throwable.getMessage();
        if (message != null && (message.contains("10008: Unknown Message") || message.contains("Missing permission: VIEW_CHANNEL"))) {
            LOGGER.info("GiveawayUpdateList: {} удаляем", message);
            giveawayRepositoryService.deleteGiveaway(messageId);
            instance.removeGiveaway(messageId);
        } else {
            LOGGER.error(throwable.getMessage(), throwable);
        }
    }
}