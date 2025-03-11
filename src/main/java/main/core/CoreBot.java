package main.core;

import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.GiveawayRegistry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoreBot extends ListenerAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoreBot.class.getName());

    private final UpdateController updateController;

    @Autowired
    public CoreBot(UpdateController updateController) {
        this.updateController = updateController;
    }

    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        updateController.processEvent(event);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        updateController.processEvent(event);
    }

    public void editMessage(MessageEmbed embedBuilder, long guildId, long textChannel, long messageId) {
        Guild guildById = BotStart.getJda().getGuildById(guildId);

        if (guildById != null) {
            GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
            if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
            if (textChannelById != null) {
                textChannelById
                        .editMessageEmbedsById(messageId, embedBuilder)
                        .queue(null, throwable -> {
                            String message = throwable.getMessage();
                            if (message.contains("10008: Unknown Message") || message.contains("Missing permission: VIEW_CHANNEL")) {
                                LOGGER.info("Delete Giveaway {}", messageId);
                                updateController.getGiveawayRepositoryService().deleteGiveaway(messageId);
                                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                                instance.removeGiveaway(messageId);
                            }
                        });
            }
        }
    }

    public void sendMessage(MessageEmbed embedBuilder, Long guildId, Long textChannel, List<Button> buttons) {
        Guild guildById = BotStart.getJda().getGuildById(guildId);

        if (guildById != null) {
            GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
            if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
            if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
            if (textChannelById != null) {
                textChannelById
                        .sendMessageEmbeds(embedBuilder)
                        .setActionRow(buttons)
                        .queue(null, throwable -> {
                            LOGGER.error(throwable.getMessage(), throwable);
                        });
            }
        }
    }

    public void sendMessage(JDA jda, Long guildId, Long textChannel, String text) {
        Guild guildById = jda.getGuildById(guildId);

        if (guildById != null) {
            GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
            if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
            if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
            if (textChannelById != null) {
                textChannelById.sendMessage(text).queue(null, throwable -> {
                    LOGGER.error(throwable.getMessage(), throwable);
                });
            }
        }
    }

    public void sendMessage(JDA jda, String userId, MessageEmbed messageEmbed) {
        RestAction<User> action = jda.retrieveUserById(userId);

        action.queue(user -> user.openPrivateChannel().queue(channel ->
                channel.sendMessageEmbeds(messageEmbed).queue(null, throwable -> {
                    if (throwable != null) {
                        if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
                            LOGGER.error("50007: Cannot send messages to this user", throwable);
                        }
                    }
                })), throwable -> {
            if (throwable != null) {
                if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
                    LOGGER.error("50007: Cannot send messages to this user", throwable);
                }
            }
        });
    }
}