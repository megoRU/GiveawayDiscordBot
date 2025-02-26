package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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

        if (giveawayList.size() > 1 && id == null) {
            String giveawayStopCommand = jsonParsers.getLocale("giveaway_stop_command", guildId);
            event.reply(giveawayStopCommand).setEphemeral(true).queue();
        } else if (giveawayList.size() > 1 && !id.matches("[0-9]+")) {
            String slashStopNoHas = jsonParsers.getLocale("id_must_be_a_number", guildId);
            event.reply(slashStopNoHas).setEphemeral(true).queue();
        } else if (giveawayList.size() > 1) {
            Optional<Giveaway> optionalGiveaway = giveawayList.stream()
                    .filter(giveaway -> giveaway.getGiveawayData().getMessageId() == Long.parseLong(id))
                    .findFirst();

            if (optionalGiveaway.isPresent()) {
                Giveaway giveaway = optionalGiveaway.get();
                if (winners == -1) {
                    GiveawayData giveawayData = giveaway.getGiveawayData();
                    int countWinners = giveawayData.getCountWinners();
                    giveaway.stopGiveaway(countWinners);
                } else {
                    giveaway.stopGiveaway(winners);
                }

                String slashStopNoHas = jsonParsers.getLocale("slash_stop", guildId);
                event.reply(slashStopNoHas).setEphemeral(true).queue();
            } else {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.reply(giveawayNotFoundById).setEphemeral(true).queue();
            }
        } else {
            Giveaway giveaway = giveawayList.getFirst();
            GiveawayData giveawayData = giveaway.getGiveawayData();

            //Это для того чтобы когда мы останавливаем Giveaway повторно
            if (giveaway.isFinishGiveaway()) {
                EmbedBuilder errorsAgain = new EmbedBuilder();
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                String errorsDescriptionsAgain = jsonParsers.getLocale("errors_descriptions_again", guildId);
                errorsAgain.setColor(Color.RED);
                errorsAgain.setTitle(errorsWithApi);
                errorsAgain.setDescription(errorsDescriptionsAgain);
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                event.replyEmbeds(errorsAgain.build()).setActionRow(buttons).setEphemeral(true).queue();
                return;
            }

            String slashStop = jsonParsers.getLocale("slash_stop", guildId);
            EmbedBuilder stop = new EmbedBuilder();
            stop.setColor(Color.GREEN);
            stop.setDescription(slashStop);
            event.replyEmbeds(stop.build()).setEphemeral(true).queue();

            if (winners == -1) {
                int countWinners = giveawayData.getCountWinners();
                giveaway.stopGiveaway(countWinners);
            } else {
                giveaway.stopGiveaway(winners);
            }
        }
    }
}