package main.controller;

import lombok.Getter;
import main.core.CoreBot;
import main.core.events.*;
import main.model.repository.*;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Getter
@Component
public class UpdateController {

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;

    //LOGGER
    private final static Logger LOGGER = LoggerFactory.getLogger(UpdateController.class.getName());

    //CORE
    private CoreBot coreBot;

    @Autowired
    public UpdateController(ActiveGiveawayRepository activeGiveawayRepository,
                            ParticipantsRepository participantsRepository,
                            ListUsersRepository listUsersRepository,
                            SchedulingRepository schedulingRepository,
                            SettingsRepository settingsRepository,
                            GiveawayRepositoryService giveawayRepositoryService) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.schedulingRepository = schedulingRepository;
        this.settingsRepository = settingsRepository;
        this.giveawayRepositoryService = giveawayRepositoryService;
    }

    public void registerBot(CoreBot coreBot) {
        this.coreBot = coreBot;
    }

    @Transactional
    public void processEvent(Object event) {
        distributeEventsByType(event);
    }

    private void distributeEventsByType(Object event) {
        if (event instanceof SlashCommandInteractionEvent slashCommandInteractionEvent) {
            LOGGER.info(slashCommandInteractionEvent.getName());
            slashEvent(slashCommandInteractionEvent);
        } else if (event instanceof ButtonInteractionEvent buttonInteractionEvent) {
            LOGGER.info(buttonInteractionEvent.getInteraction().getButton().getLabel());
            buttonEvent(buttonInteractionEvent);
        } else if (event instanceof GuildJoinEvent guildJoinEvent) {
            joinEvent(guildJoinEvent);
        } else if (event instanceof MessageReactionAddEvent messageReactionAddEvent) {
            reactionEvent(messageReactionAddEvent);
        } else if (event instanceof StringSelectInteractionEvent stringSelectInteractionEvent) {
            selectMenuEvent(stringSelectInteractionEvent);
        } else if (event instanceof GuildLeaveEvent guildLeaveEvent) {
            LOGGER.info(guildLeaveEvent.getGuild().getId());
            leaveEvent(guildLeaveEvent);
        } else if (event instanceof MessageDeleteEvent messageDeleteEvent) {
            messageDeleteEvent(messageDeleteEvent);
        }
    }

    private void selectMenuEvent(StringSelectInteractionEvent stringSelectInteractionEvent) {
        SelectMenuInteraction selectMenuInteraction = new SelectMenuInteraction(activeGiveawayRepository, schedulingRepository);
        selectMenuInteraction.handle(stringSelectInteractionEvent);
    }

    private void messageDeleteEvent(MessageDeleteEvent messageDeleteEvent) {
        DeleteEvent deleteEvent = new DeleteEvent(giveawayRepositoryService);
        deleteEvent.handle(messageDeleteEvent);
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
            case "list" -> {
                ListCommand listCommand = new ListCommand();
                listCommand.handle(event);
            }
            case "endmessage" -> {
                EndMessageCommand endMessageCommand = new EndMessageCommand(settingsRepository);
                endMessageCommand.handle(event);
            }
            case "start" -> {
                StartCommand startCommand = new StartCommand(giveawayRepositoryService);
                startCommand.start(event, this);
            }
            case "stop" -> {
                StopCommand stopCommand = new StopCommand();
                stopCommand.stop(event);
            }
            case "predefined" -> {
                PredefinedCommand predefinedCommand = new PredefinedCommand(giveawayRepositoryService);
                predefinedCommand.predefined(event, this);
            }
            case "settings" -> {
                SettingsCommand settingsCommand = new SettingsCommand(settingsRepository);
                settingsCommand.language(event);
            }
            case "reroll" -> {
                RerollCommand rerollCommand = new RerollCommand(listUsersRepository);
                rerollCommand.reroll(event);
            }
            case "edit" -> {
                EditGiveawayCommand editGiveawayCommand = new EditGiveawayCommand(activeGiveawayRepository, schedulingRepository, this);
                editGiveawayCommand.editGiveaway(event);
            }
            case "scheduling" -> {
                SchedulingCommand schedulingCommand = new SchedulingCommand(schedulingRepository);
                schedulingCommand.scheduling(event);
            }
            case "participants" -> {
                ParticipantsCommand participantsCommand = new ParticipantsCommand(listUsersRepository, participantsRepository);
                participantsCommand.participants(event);
            }
            case "cancel" -> {
                CancelCommand cancelCommand = new CancelCommand(schedulingRepository, activeGiveawayRepository);
                cancelCommand.cancel(event);
            }
            case "check" -> {
                CheckPermissions checkPermissions = new CheckPermissions();
                checkPermissions.check(event);
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
        LeaveEvent leaveEvent = new LeaveEvent(activeGiveawayRepository, schedulingRepository, settingsRepository);
        leaveEvent.leave(event);
    }

    private void reactionEvent(@NotNull MessageReactionAddEvent event) {
        ReactionEvent reactionEvent = new ReactionEvent();
        reactionEvent.reaction(event, this);
    }

    public void setView(EmbedBuilder embedBuilder, final long guildId, final long textChannel, final long messageId) {
        coreBot.editMessage(embedBuilder, guildId, textChannel, messageId);
    }

    public void setView(MessageEmbed messageEmbed, final long guildId, final long textChannel, long messageId) {
        coreBot.editMessage(messageEmbed, guildId, textChannel, messageId);
    }

    public void setView(MessageEmbed embedBuilder, String messageContent, Long guildId, Long textChannel) {
        coreBot.sendMessage(embedBuilder, messageContent, guildId, textChannel);
    }

    public void setView(JDA jda, String messageContent, Long guildId, Long textChannel) {
        coreBot.sendMessage(jda, guildId, textChannel, messageContent);
    }

    public void setView(MessageEmbed embedBuilder, Long guildId, Long textChannel, List<Button> buttons) {
        coreBot.sendMessage(embedBuilder, guildId, textChannel, buttons);
    }

    public void setView(JDA jda, String userId, MessageEmbed messageEmbed) {
        coreBot.sendMessage(jda, userId, messageEmbed);
    }
}