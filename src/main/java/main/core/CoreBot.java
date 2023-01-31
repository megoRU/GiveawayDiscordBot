package main.core;

import jakarta.annotation.PostConstruct;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CoreBot extends ListenerAdapter {

    private final UpdateController updateController;

    @Autowired
    public CoreBot(UpdateController updateController) {
        this.updateController = updateController;
    }

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
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
            e.printStackTrace();
            if (e.getMessage().contains("10008: Unknown Message")
                    || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                System.out.println(e.getMessage() + " удаляем!");
                updateController.getActiveGiveawayRepository().deleteActiveGiveaways(guildId);
                GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
            } else {
                e.printStackTrace();
            }
        }
    }
}
