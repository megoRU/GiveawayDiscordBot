package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.giveaway.GiveawayUtils;
import main.giveaway.ParticipantDTO;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ParticipantsGrabber {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantsGrabber.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    public Set<ParticipantDTO> get(@NotNull ActiveGiveaways activeGiveaway) throws Exception {
        long guildId = activeGiveaway.getGuildId();
        long channelId = activeGiveaway.getChannelId();
        boolean isForSpecificRole = activeGiveaway.getIsForSpecificRole();
        long messageId = activeGiveaway.getMessageId();

        JDA jda = BotStart.getJda();

        if (jda != null) {
            try {
                Guild guildById = jda.getGuildById(guildId);

                if (guildById == null) {
                    giveawayRepositoryService.deleteGiveaway(messageId);
                    return new HashSet<>();
                } else {
                    TextChannel textChannelById = guildById.getTextChannelById(channelId);

                    if (textChannelById == null) {
                        giveawayRepositoryService.deleteGiveaway(messageId);
                        return new HashSet<>();
                    } else {
                        Message message = textChannelById.retrieveMessageById(messageId).complete(true);
                        List<MessageReaction> reactions = message.getReactions()
                                .stream()
                                .filter(messageReaction -> messageReaction.getEmoji().getName().equals(GiveawayUtils.TADA))
                                .toList();

                        if (!reactions.isEmpty()) {
                            MessageReaction reaction = reactions.getFirst();

                            //Добавление всех пользователей
                            List<User> users = new ArrayList<>();

                            reaction.retrieveUsers().forEach(u -> {
                                if (!u.isBot()) {
                                    users.add(u);
                                }
                            });

                            List<User> usersBackup = new ArrayList<>(users);

                            if (isForSpecificRole) {
                                Role roleGiveaway = jda.getRoleById(activeGiveaway.getRoleId());
                                if (roleGiveaway != null) {
                                    for (User user : usersBackup) {
                                        try {
                                            Member member = guildById.retrieveMemberById(user.getId()).complete();

                                            if (member != null && !member.getRoles().contains(roleGiveaway)) {
                                                users.remove(user);
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("Error retrieving member {}: {}", user.getId(), e.getMessage());
                                        }
                                    }
                                }

                                return users.stream()
                                        .map(user -> new ParticipantDTO(user.getIdLong(), user.getName()))
                                        .collect(Collectors.toSet());
                            } else {
                                return users.stream()
                                        .map(user -> new ParticipantDTO(user.getIdLong(), user.getName()))
                                        .collect(Collectors.toSet());
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
                    }
                }

                LOGGER.warn("Не удалось обновить участников Giveaway, повторим позже", e);
            }
        } else {
            throw new Exception("JDA is NULL");
        }

        throw new Exception("JDA is NULL");
    }
}