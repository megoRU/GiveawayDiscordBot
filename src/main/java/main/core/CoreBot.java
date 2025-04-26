package main.core;

import main.controller.UpdateController;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
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
}