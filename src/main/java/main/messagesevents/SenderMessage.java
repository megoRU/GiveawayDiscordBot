package main.messagesevents;

import main.config.BotStartConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.List;

public interface SenderMessage {

    static void sendMessage(MessageEmbed embedBuilder, Long guildId, Long textChannel) {
        try {
            Guild guildById = BotStartConfig.getJda().getGuildById(guildId);
            if (guildById != null) {
                GuildMessageChannel textChannelById = guildById.getTextChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getNewsChannelById(textChannel);
                if (textChannelById == null) textChannelById = guildById.getThreadChannelById(textChannel);
                if (textChannelById != null) {
                    textChannelById
                            .sendMessageEmbeds(embedBuilder)
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sendMessage(MessageEmbed embedBuilder, String messageContent, Long guildId, Long textChannel) {
        try {
            Guild guildById = BotStartConfig.getJda().getGuildById(guildId);
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
            e.printStackTrace();
        }
    }

    static void sendMessage(MessageEmbed embedBuilder, Long guildId, Long textChannel, List<Button> buttons) {
        try {
            Guild guildById = BotStartConfig.getJda().getGuildById(guildId);
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
            e.printStackTrace();
        }
    }

    static void sendPrivateMessage(JDA jda, String userId, MessageEmbed messageEmbed) {
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

//    static void sendPrivateMessageWithButtons(JDA jda, String userId, MessageEmbed messageEmbed, List<Button> actionRows) {
//        RestAction<User> action = jda.retrieveUserById(userId);
//        action.submit()
//                .thenCompose((user) -> user.openPrivateChannel().submit())
//                .thenCompose((channel) -> channel.sendMessageEmbeds(messageEmbed)
//                        .setActionRow(actionRows)
//                        .submit())
//                .whenComplete((v, throwable) -> {
//                    if (throwable != null) {
//                        if (throwable.getMessage().contains("50007: Cannot send messages to this user")) {
//                            System.out.println("50007: Cannot send messages to this user");
//                        }
//                    }
//                });
//    }
}