package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class StopCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void stop(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);

        if (giveawayList.isEmpty()) {
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
                Giveaway giveaway = instance.getGiveaway(giveawayId);

                if (giveaway != null) {
                    handleStopCommand(event, giveaway, winners);
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
                Giveaway giveaway = giveawayList.getFirst();
                handleStopCommand(event, giveaway, winners);
            }
        }
    }

    private void handleStopCommand(SlashCommandInteractionEvent event, Giveaway giveaway, long winners) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        long guildId = giveaway.getGuildId();

        if (giveaway.isFinishGiveaway()) {
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

        int countWinners = giveawayData.getCountWinners();
        int participantSize = giveawayData.getParticipantSize();

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