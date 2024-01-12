package main.controller;

import main.core.events.*;
import main.giveaway.GiveawayEnd;
import main.giveaway.GiveawayMessageHandler;
import main.giveaway.GiveawaySaving;
import main.model.repository.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UpdateController {

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SettingsRepository settingsRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;
    private final SchedulingRepository schedulingRepository;

    //Service
    private final GiveawayMessageHandler giveawayMessageHandler;
    private final GiveawaySaving giveawaySaving;
    private final GiveawayEnd giveawayEnd;


    @Autowired
    public UpdateController(ActiveGiveawayRepository activeGiveawayRepository,
                            SettingsRepository settingsRepository,
                            ParticipantsRepository participantsRepository,
                            ListUsersRepository listUsersRepository,
                            SchedulingRepository schedulingRepository,
                            GiveawayMessageHandler giveawayMessageHandler,
                            GiveawaySaving giveawaySaving,
                            GiveawayEnd giveawayEnd) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.settingsRepository = settingsRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.schedulingRepository = schedulingRepository;
        this.giveawayMessageHandler = giveawayMessageHandler;
        this.giveawaySaving = giveawaySaving;
        this.giveawayEnd = giveawayEnd;
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
            reactionEvent((MessageReactionAddEvent) event);
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
                StartCommand startCommand = new StartCommand(listUsersRepository, activeGiveawayRepository, participantsRepository, schedulingRepository, giveawayMessageHandler, giveawaySaving, giveawayEnd);
                startCommand.start(event);
            }
            case "stop" -> {
                StopCommand stopCommand = new StopCommand();
                stopCommand.stop(event);
            }
            case "predefined" -> {
                PredefinedCommand predefinedCommand = new PredefinedCommand(listUsersRepository, activeGiveawayRepository, participantsRepository, giveawayMessageHandler, giveawaySaving, giveawayEnd);
                predefinedCommand.predefined(event);
            }

            case "settings" -> {
                SettingsCommand settingsCommand = new SettingsCommand(settingsRepository);
                settingsCommand.language(event);
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
                ChangeCommand changeCommand = new ChangeCommand(activeGiveawayRepository, giveawayMessageHandler);
                changeCommand.change(event);
            }
            case "scheduling" -> {
                SchedulingCommand schedulingCommand = new SchedulingCommand(schedulingRepository);
                schedulingCommand.scheduling(event);
            }
            case "participants" -> {
                ParticipantsCommand participantsCommand = new ParticipantsCommand(listUsersRepository);
                participantsCommand.participants(event);
            }
            case "patreon" -> {
                PatreonCommand patreonCommand = new PatreonCommand();
                patreonCommand.patreon(event);
            }
            case "cancel" -> {
                CancelCommand cancelCommand = new CancelCommand(schedulingRepository, activeGiveawayRepository);
                cancelCommand.cancel(event);
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
            ButtonChangeLanguage buttonChangeLanguage = new ButtonChangeLanguage(settingsRepository);
            buttonChangeLanguage.change(event);
        }
    }

    private void joinEvent(@NotNull GuildJoinEvent event) {
        JoinEvent joinEvent = new JoinEvent();
        joinEvent.join(event);
    }

    private void leaveEvent(@NotNull GuildLeaveEvent event) {
        LeaveEvent leaveEvent = new LeaveEvent(activeGiveawayRepository, settingsRepository);
        leaveEvent.leave(event);
    }

    private void reactionEvent(@NotNull MessageReactionAddEvent event) {
        ReactionEvent reactionEvent = new ReactionEvent(giveawayMessageHandler);
        reactionEvent.reaction(event);
    }
}