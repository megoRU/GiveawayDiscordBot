package main.controller;

import lombok.Getter;
import main.core.CoreBot;
import main.core.events.*;
import main.model.repository.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.logging.Logger;

@Getter
@Component
public class UpdateController {

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationRepository notificationRepository;
    private final ListUsersRepository listUsersRepository;

    //LOGGER
    private final static Logger LOGGER = Logger.getLogger(UpdateController.class.getName());

    //CORE
    private CoreBot coreBot;

    @Autowired
    public UpdateController(ActiveGiveawayRepository activeGiveawayRepository, LanguageRepository languageRepository, ParticipantsRepository participantsRepository, NotificationRepository notificationRepository, ListUsersRepository listUsersRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.languageRepository = languageRepository;
        this.participantsRepository = participantsRepository;
        this.notificationRepository = notificationRepository;
        this.listUsersRepository = listUsersRepository;
    }

    public void registerBot(CoreBot coreBot) {
        this.coreBot = coreBot;
    }

    public void processEvent(Object event) {
        distributeEventsByType(event);
    }

    private void distributeEventsByType(Object event) {
        if (event instanceof SlashCommandInteractionEvent) {
            slashEvent((SlashCommandInteractionEvent) event);
        } else if (event instanceof ButtonInteractionEvent) {
            buttonEvent((ButtonInteractionEvent) event);
        } else if (event instanceof GuildJoinEvent) {
            joinEvent((GuildJoinEvent) event);
        } else if (event instanceof MessageReactionAddEvent) {
            messageReactionEvent((MessageReactionAddEvent) event);
        } else if (event instanceof GuildLeaveEvent) {
            leaveEvent((GuildLeaveEvent) event);
        }
    }

    private void slashEvent(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() != null && event.getChannelType().isThread()) {
            LanguageHandler languageHandler = new LanguageHandler();
            languageHandler.handler(event, "start_in_thread");
            return;
        }

        switch (event.getName()) {
            case "help" -> {
                HelpCommand helpCommand = new HelpCommand();
                helpCommand.help(event);
            }
            case "start" -> {
                StartCommand startCommand = new StartCommand(listUsersRepository, activeGiveawayRepository, participantsRepository);
                startCommand.start(event, this);
            }
            case "stop" -> {
                StopCommand stopCommand = new StopCommand();
                stopCommand.stop(event);
            }
            case "predefined" -> {
                PredefinedCommand predefinedCommand = new PredefinedCommand(listUsersRepository, activeGiveawayRepository, participantsRepository);
                predefinedCommand.predefined(event, this);
            }

            case "language" -> {
                LanguageCommand languageCommand = new LanguageCommand(languageRepository);
                languageCommand.language(event);
            }
            case "list" -> {
                ListCommand listCommand = new ListCommand(participantsRepository);
                listCommand.list(event);
            }
            case "reroll" -> {
                RerollCommand rerollCommand = new RerollCommand(listUsersRepository);
                rerollCommand.reroll(event);
            }
            case "change" -> {
                ChangeCommand changeCommand = new ChangeCommand(activeGiveawayRepository);
                changeCommand.change(event);
            }
            case "participants" -> {
                ParticipantsCommand participantsCommand = new ParticipantsCommand(listUsersRepository);
                participantsCommand.participants(event);
            }
            case "patreon" -> {
                PatreonCommand patreonCommand = new PatreonCommand();
                patreonCommand.patreon(event);
            }
            case "check-bot-permission" -> {
                CheckBot checkBot = new CheckBot();
                checkBot.check(event);
            }
        }
    }

    private void buttonEvent(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + ButtonChangeLanguage.CHANGE_LANGUAGE)) {
            ButtonChangeLanguage buttonChangeLanguage = new ButtonChangeLanguage(languageRepository);
            buttonChangeLanguage.change(event);
        }
    }

    private void joinEvent(@NotNull GuildJoinEvent event) {
        JoinEvent joinEvent = new JoinEvent();
        joinEvent.join(event);
    }

    private void leaveEvent(@NotNull GuildLeaveEvent event) {
        LeaveEvent leaveEvent = new LeaveEvent(activeGiveawayRepository, languageRepository);
        leaveEvent.leave(event);
    }

    private void messageReactionEvent(@NotNull MessageReactionAddEvent event) {
        ReactionEvent reactionEvent = new ReactionEvent();
        reactionEvent.reaction(event);
    }

    public void setView(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        coreBot.editMessage(embedBuilder, guildId, textChannel);
    }
}