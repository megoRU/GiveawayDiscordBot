package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;

@Service
@AllArgsConstructor
public class CancelCommand {

    private final SchedulingRepository schedulingRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        Scheduling scheduling = schedulingRepository.findByCreatedUserIdAndGuildId(userId, guildId);
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByCreatedUserIdAndGuildId(userId, guildId);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        EmbedBuilder cancel = new EmbedBuilder();
        cancel.setColor(Color.GREEN);

        if (scheduling != null) {
            String cancelSchedulingGiveaway = jsonParsers.getLocale("cancel_scheduling_giveaway", guildId);

            schedulingRepository.deleteById(guildId);
            instance.removeGuildFromGiveaway(guildId);
            cancel.setDescription(cancelSchedulingGiveaway);
        } else if (activeGiveaways != null) {
            String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);

            activeGiveawayRepository.deleteById(guildId);
            instance.removeGuildFromGiveaway(guildId);
            cancel.setDescription(cancelGiveaway);
        } else {
            String cancelSchedulingGiveaway = jsonParsers.getLocale("no_active_giveaway", guildId);

            cancel.setDescription(cancelSchedulingGiveaway);
        }

        event.replyEmbeds(cancel.build()).setEphemeral(true).queue();
    }
}