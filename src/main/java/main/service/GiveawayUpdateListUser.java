package main.service;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.core.events.ReactionEvent;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
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

    public void updateGiveawayByGuild(@NotNull Giveaway giveaway) {
        GiveawayData giveawayData = giveaway.getGiveawayData();

        long guildId = giveaway.getGuildId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        long messageId = giveawayData.getMessageId();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        JDA jda = BotStart.getJda();

        if (jda != null) {
            if (instance.hasGiveaway(messageId)) {
                long channelId = giveaway.getTextChannelId();
                //System.out.println("Guild ID: " + guildIdLong);

                List<MessageReaction> reactions = null;
                TextChannel textChannelById;
                try {
                    Guild guildById = jda.getGuildById(guildId);
                    if (guildById != null) {
                        textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            reactions = textChannelById
                                    .retrieveMessageById(messageId)
                                    .complete()
                                    .getReactions()
                                    .stream()
                                    .filter(messageReaction -> messageReaction.getEmoji().getName().equals(ReactionEvent.TADA))
                                    .toList();
                        }

                        //-1 because one Bot
                        if (instance.hasGiveaway(messageId) &&
                                reactions != null &&
                                !reactions.isEmpty() &&
                                reactions.getFirst().getCount() - 1 != giveawayData.getParticipantSize()) {
                            for (MessageReaction reaction : reactions) {
                                Map<String, User> userList = reaction
                                        .retrieveUsers()
                                        .complete()
                                        .stream()
                                        .filter(user -> !user.isBot())
                                        .collect(Collectors.toMap(User::getId, user -> user));

                                if (isForSpecificRole) {
                                    try {
                                        Map<String, User> localUserMap = new HashMap<>(userList); //bad practice but it`s work
                                        Role roleGiveaway = jda.getRoleById(giveawayData.getRoleId());
                                        for (Map.Entry<String, User> entry : localUserMap.entrySet()) {
                                            Guild guild = jda.getGuildById(guildId);
                                            if (guild != null) {
                                                try {
                                                    Member member = guild.retrieveMemberById(entry.getKey()).complete();
                                                    if (member != null) {
                                                        boolean contains = member.getRoles().contains(roleGiveaway);
                                                        if (!contains) {
                                                            userList.remove(entry.getKey());
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    //Если пользователя нет в Гильдии удаляем из списка
                                                    if (e.getMessage().contains("10007: Unknown Member")) {
                                                        userList.remove(entry.getKey());
                                                    } else {
                                                        LOGGER.error(e.getMessage(), e);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                }

                                //Перебираем Users в реакциях
                                if (instance.hasGiveaway(messageId)) {
                                    giveaway.addUser(userList.values().stream().toList());
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