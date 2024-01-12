package main.giveaway;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class GiveawayMessageHandler {

    private static final Logger LOGGER = Logger.getLogger(GiveawayMessageHandler.class.getName());
    private final ActiveGiveawayRepository activeGiveawayRepository;

    public void editMessage(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        try {
            Guild guildById = BotStart.getJda().getGuildById(guildId);
            if (guildById != null) {
                GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
                if (textChannelById != null) {
                    GiveawayRegistry instance = GiveawayRegistry.getInstance();
                    Giveaway giveaway = instance.getGiveaway(guildId);
                    if (giveaway != null) {
                        textChannelById
                                .retrieveMessageById(giveaway.getMessageId())
                                .complete()
                                .editMessageEmbeds(embedBuilder.build())
                                .submit();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
            if (e.getMessage().contains("10008: Unknown Message")
                    || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                System.out.println(e.getMessage() + " удаляем!");
                activeGiveawayRepository.deleteById(guildId);
                GiveawayRegistry.getInstance().removeGiveaway(guildId);
            } else {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    public void editMessage(MessageEmbed messageEmbed, long guildId, long textChannel, long messageId) {
        try {
            Guild guildById = BotStart.getJda().getGuildById(guildId);
            if (guildById != null) {
                GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .editMessageEmbedsById(messageId, messageEmbed)
                            .queue();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    public void sendMessage(MessageEmbed embedBuilder, String messageContent, long guildId, long textChannel) {
        try {
            Guild guildById = BotStart.getJda().getGuildById(guildId);
            if (guildById != null) {
                GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .sendMessageEmbeds(embedBuilder)
                            .setContent(messageContent)
                            .queue();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    public void sendMessage(MessageEmbed embedBuilder, long guildId, long textChannel, List<Button> buttons) {
        try {
            Guild guildById = BotStart.getJda().getGuildById(guildId);
            if (guildById != null) {
                GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .sendMessageEmbeds(embedBuilder)
                            .setActionRow(buttons)
                            .queue();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    public void sendMessage(JDA jda, long userId, MessageEmbed messageEmbed) {
        RestAction<User> action = jda.retrieveUserById(userId);
        action.submit()
                .thenCompose((user) -> user.openPrivateChannel().submit())
                .thenCompose((channel) -> channel.sendMessageEmbeds(messageEmbed).submit())
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
                            System.out.println("50007: Cannot send messages to this user");
                        }
                    }
                });
    }
}
