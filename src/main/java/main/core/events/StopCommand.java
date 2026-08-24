package main.core.events;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class StopCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final UpdateController updateController;

    public void stop(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);

        List<ActiveGiveaways> giveawayList = activeGiveawayRepository.findByGuildId(guildId);

        if (giveawayList == null || giveawayList.isEmpty()) {
            String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", guildId);
            EmbedBuilder notHas = new EmbedBuilder();
            notHas.setColor(Color.GREEN);
            notHas.setDescription(slashStopNoHas);
            event.replyEmbeds(notHas.build()).setEphemeral(true).queue();
            return;
        }

        if (id != null) {
            if (id.matches("[0-9]+")) {
                long giveawayId = Long.parseLong(id);
                ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByMessageId(giveawayId);

                if (activeGiveaways != null && activeGiveaways.getGuildId().equals(guildId)) {
                    handleStopCommand(event, activeGiveaways, winners);
                } else {
                    String selectMenuGiveawayNotFound = jsonParsers.getLocale("select_menu_giveaway_not_found", guildId);
                    event.reply(selectMenuGiveawayNotFound).setEphemeral(true).queue();
                }
            } else {
                String idMustBeANumber = jsonParsers.getLocale("id_must_be_a_number", guildId);
                event.reply(idMustBeANumber).setEphemeral(true).queue();
            }
        } else {
            if (giveawayList.size() > 1) {
                String giveawayStopCommand = jsonParsers.getLocale("giveaway_stop_command", guildId);
                event.reply(giveawayStopCommand).setEphemeral(true).queue();
            } else {
                ActiveGiveaways activeGiveaways = giveawayList.getFirst();
                handleStopCommand(event, activeGiveaways, winners);
            }
        }
    }

    private void handleStopCommand(SlashCommandInteractionEvent event, ActiveGiveaways activeGiveaways, long winners) {
        long guildId = activeGiveaways.getGuildId();

        if (activeGiveaways.isFinish()) {
            EmbedBuilder errorsAgain = new EmbedBuilder();
            String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
            String errorsDescriptionsAgain = jsonParsers.getLocale("errors_descriptions_again", guildId);
            errorsAgain.setColor(Color.RED);
            errorsAgain.setTitle(errorsWithApi);
            errorsAgain.setDescription(errorsDescriptionsAgain);
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/MhEzJNDf", "Support"));
            event.replyEmbeds(errorsAgain.build()).setComponents(ActionRow.of(buttons)).setEphemeral(true).queue();
            return;
        }

        Set<Long> participantsList = activeGiveaways.getParticipants() != null ? activeGiveaways.getParticipants()
                .stream()
                .map(Participants::getUserId)
                .collect(Collectors.toSet()) : Collections.emptySet();

        Giveaway giveaway = new Giveaway(
                activeGiveaways.getGuildId(),
                activeGiveaways.getChannelId(),
                activeGiveaways.getCreatedUserId(),
                activeGiveaways.isFinish(),
                activeGiveaways.getMessageId(),
                activeGiveaways.getCountWinners(),
                activeGiveaways.getRoleId(),
                activeGiveaways.getIsForSpecificRole(),
                activeGiveaways.getUrlImage(),
                activeGiveaways.getTitle(),
                activeGiveaways.getEndGiveawayDate(),
                activeGiveaways.getMinParticipants() == null ? 1 : activeGiveaways.getMinParticipants(),
                giveawayRepositoryService,
                updateController
        );
        giveaway.setParticipantsList(participantsList);

        int countWinners = giveaway.getCountWinners();
        int participantSize = giveaway.getParticipantSize();

        if (winners == -1) {
            stop(event, giveaway, countWinners, guildId, countWinners, participantSize);
        } else {
            stop(event, giveaway, winners, guildId, countWinners, participantSize);
        }
    }

    private void stop(SlashCommandInteractionEvent event, Giveaway giveaway, long winners, long guildId, int countWinners, int participantSize) {
        if (winners <= participantSize) {
            giveaway.stopGiveaway(countWinners);
            String slashStopNoHas = jsonParsers.getLocale("slash_stop", guildId);
            event.reply(slashStopNoHas).setEphemeral(true).queue();
        } else {
            String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
            event.reply(giftNotEnoughUsers).setEphemeral(true).queue();
        }
    }
}