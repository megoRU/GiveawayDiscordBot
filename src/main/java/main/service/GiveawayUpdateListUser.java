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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GiveawayUpdateListUser {

    private static final Logger LOGGER = Logger.getLogger(GiveawayUpdateListUser.class.getName());

    private final GiveawayRepositoryService giveawayRepositoryService;

    public void updateGiveawayByGuild(@NotNull Giveaway giveaway) {
        GiveawayData giveawayData = giveaway.getGiveawayData();

        long guildIdLong = giveaway.getGuildId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        long messageId = giveawayData.getMessageId();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        boolean hasGiveaway = instance.hasGiveaway(guildIdLong);

        JDA jda = BotStart.getJda();

        if (jda != null) {
            if (hasGiveaway) {
                long channelId = giveaway.getTextChannelId();
                //System.out.println("Guild ID: " + guildIdLong);

                List<MessageReaction> reactions = null;
                TextChannel textChannelById;
                try {
                    Guild guildById = jda.getGuildById(guildIdLong);
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
                        hasGiveaway = instance.hasGiveaway(guildIdLong);

                        //-1 because one Bot
                        if (hasGiveaway &&
                                reactions != null &&
                                !reactions.isEmpty() &&
                                reactions.get(0).getCount() - 1 != giveawayData.getParticipantSize()) {
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
                                            Guild guild = jda.getGuildById(guildIdLong);
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
                                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                }

                                hasGiveaway = instance.hasGiveaway(guildIdLong);

                                //System.out.println("UserList count: " + userList);
                                //Перебираем Users в реакциях
                                for (Map.Entry<String, User> entry : userList.entrySet()) {
                                    if (!hasGiveaway) return;
                                    giveaway.addUser(entry.getValue());
                                    //System.out.println("User id: " + user.getIdLong());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("10008: Unknown Message")
                            || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                        System.out.println("updateUserList() " + e.getMessage() + " удаляем!");
                        giveawayRepositoryService.deleteGiveaway(guildIdLong);
                        GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildIdLong);
                    } else {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
    }
}