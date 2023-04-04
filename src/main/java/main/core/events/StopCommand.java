package main.core.events;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StopCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void stop(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getId();
        var guildIdLong = event.getGuild().getIdLong();

        Giveaway giveaway = GiveawayRegistry.getInstance().getGiveaway(guildIdLong);
        if (giveaway == null) {
            String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", guildId);
            EmbedBuilder notHas = new EmbedBuilder();
            notHas.setColor(Color.GREEN);
            notHas.setDescription(slashStopNoHas);
            event.replyEmbeds(notHas.build()).queue();
            return;
        }

        if (event.getOptions().isEmpty()) {
            if (!giveaway.isHasFutureTasks()) {
                EmbedBuilder errorsAgain = new EmbedBuilder();
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                String errorsDescriptionsAgain = jsonParsers.getLocale("errors_descriptions_again", guildId);
                errorsAgain.setColor(Color.RED);
                errorsAgain.setTitle(errorsWithApi);
                errorsAgain.setDescription(errorsDescriptionsAgain);
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                event.replyEmbeds(errorsAgain.build()).setActionRow(buttons).queue();
                return;
            }
            String slashStop = jsonParsers.getLocale("slash_stop", guildId);
            EmbedBuilder stop = new EmbedBuilder();
            stop.setColor(Color.GREEN);
            stop.setDescription(slashStop);
            event.replyEmbeds(stop.build()).queue();
            giveaway.stopGiveaway(giveaway.getCountWinners());
            return;
        }

        if (!event.getOptions().get(0).getAsString().matches("\\d{1,2}")) {
            String slashErrors = jsonParsers.getLocale("slash_errors", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.RED);
            errors.setDescription(slashErrors);
            event.replyEmbeds(errors.build()).queue();
            return;
        }

        EmbedBuilder stop = new EmbedBuilder();
        Long count = event.getOption("count", OptionMapping::getAsLong);
        boolean isHasErrors = false;
        if (count == null) return;

        if (giveaway.getListUsersSize() <= count) {
            isHasErrors = true;
        }

        if (!isHasErrors) {
            String slashStop = jsonParsers.getLocale("slash_stop", guildId);
            stop.setColor(Color.GREEN);
            stop.setDescription(slashStop);
            event.replyEmbeds(stop.build()).queue();
        } else {
            String slashStopErrors = jsonParsers.getLocale("slash_stop_errors", guildId);
            stop.setColor(Color.RED);
            stop.setDescription(slashStopErrors);
            event.replyEmbeds(stop.build()).queue();
        }
        giveaway.stopGiveaway(Integer.parseInt(event.getOptions().get(0).getAsString()));
    }
}