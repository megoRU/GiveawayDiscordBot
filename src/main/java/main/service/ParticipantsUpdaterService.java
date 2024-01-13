package main.service;

import main.core.events.ReactionEvent;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParticipantsUpdaterService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ParticipantsUpdaterService.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;

    @Autowired
    public ParticipantsUpdaterService(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void update(JDA jda) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayList = new ArrayList<>(instance.getGiveaways());
        for (Giveaway giveaway : giveawayList) {
            long guildIdLong = giveaway.getGuildId();
            boolean isForSpecificRole = giveaway.isForSpecificRole();
            long messageId = giveaway.getMessageId();

            if (jda != null) {
                if (instance.hasGiveaway(guildIdLong)) {
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

                            //-1 because one Bot
                            if (instance.hasGiveaway(guildIdLong) &&
                                    reactions != null &&
                                    !reactions.isEmpty() &&
                                    reactions.get(0).getCount() - 1 != giveaway.getListUsersSize()) {
                                for (MessageReaction reaction : reactions) {
                                    Map<String, User> userList = reaction
                                            .retrieveUsers()
                                            .complete()
                                            .stream()
                                            .filter(user -> !user.isBot())
                                            .filter(user -> !giveaway.isUserContains(user.getId()))
                                            .collect(Collectors.toMap(User::getId, user -> user));

                                    if (isForSpecificRole) {
                                        try {
                                            Map<String, User> localUserMap = new HashMap<>(userList); //bad practice but it`s work
                                            Role roleGiveaway = jda.getRoleById(giveaway.getRoleId());
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
                                                            LOGGER.error(e.getMessage(), e);
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error(e.getMessage(), e);
                                        }
                                    }

                                    //System.out.println("UserList count: " + userList);
                                    //Перебираем Users в реакциях
                                    for (Map.Entry<String, User> entry : userList.entrySet()) {
                                        if (!instance.hasGiveaway(guildIdLong)) return;
                                        giveaway.addUser(entry.getValue());
                                        //System.out.println("User id: " + user.getIdLong());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("10008: Unknown Message")
                                || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                            LOGGER.info(e.getMessage());
                            activeGiveawayRepository.deleteById(guildIdLong);
                            GiveawayRegistry.getInstance().removeGiveaway(guildIdLong);
                        } else {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }
}