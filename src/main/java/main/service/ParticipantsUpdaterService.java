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
            Long forbiddenRole = giveaway.getForbiddenRole();

            if (jda != null) {
                if (instance.hasGiveaway(guildIdLong)) {
                    long channelId = giveaway.getTextChannelId();
                    Guild guild = jda.getGuildById(guildIdLong);

                    try {
                        if (guild != null) {
                            TextChannel textChannelById = guild.getTextChannelById(channelId);
                            if (textChannelById != null) {
                                List<MessageReaction> reactions = textChannelById
                                        .retrieveMessageById(messageId)
                                        .complete()
                                        .getReactions()
                                        .stream()
                                        .filter(messageReaction -> messageReaction.getEmoji().getName().equals(ReactionEvent.TADA))
                                        .toList();

                                if (!reactions.isEmpty()) {
                                    //-1 because one Bot
                                    if (reactions.get(0).getCount() - 1 != giveaway.getListUsersSize()) {
                                        final Map<String, User> userList = new HashMap<>();

                                        MessageReaction messageReaction = reactions.get(0);
                                        messageReaction.retrieveUsers().complete().forEach(user -> {
                                            if (!user.isBot()) userList.put(user.getId(), user);
                                        });

                                        if (isForSpecificRole) {
                                            Role roleGiveaway = guild.getRoleById(giveaway.getRoleId());
                                            if (roleGiveaway == null) continue;

                                            List<Member> members = guild.loadMembers().get();

                                            for (Member member : members) {
                                                List<Long> memberRolesList = member.getRoles().stream().map(Role::getIdLong).toList();

                                                boolean contains = memberRolesList.contains(roleGiveaway.getIdLong());
                                                if (!contains) {
                                                    String memberId = member.getId();
                                                    userList.remove(memberId);
                                                }
                                            }
                                        }

                                        if (forbiddenRole != null) {
                                            Role forbidden = guild.getRoleById(giveaway.getForbiddenRole());
                                            if (forbidden == null) continue;

                                            List<Member> members = guild.loadMembers().get();

                                            for (Member member : members) {
                                                List<Long> memberRolesList = member.getRoles().stream().map(Role::getIdLong).toList();

                                                boolean contains = memberRolesList.contains(forbidden.getIdLong());
                                                if (contains) {
                                                    String memberId = member.getId();
                                                    userList.remove(memberId);
                                                }
                                            }
                                        }

                                        userList.values().forEach(user -> {
                                            if (instance.hasGiveaway(guildIdLong)) {
                                                giveaway.addUser(user);
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        boolean unknownMessage = e.getMessage().contains("10008: Unknown Message");
                        boolean missingPermission = e.getMessage().contains("Missing permission: VIEW_CHANNEL");
                        if (unknownMessage || missingPermission) {
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