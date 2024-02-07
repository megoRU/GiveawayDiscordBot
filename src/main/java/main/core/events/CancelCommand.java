package main.core.events;

import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;

@Service
public class CancelCommand {

    private final SchedulingRepository schedulingRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;

    @Autowired
    public CancelCommand(SchedulingRepository schedulingRepository, ActiveGiveawayRepository activeGiveawayRepository) {
        this.schedulingRepository = schedulingRepository;
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        schedulingRepository.deleteById(guildId);
        activeGiveawayRepository.deleteById(guildId);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.cancelGiveawayTimer(guildId);
        instance.removeGuildFromGiveaway(guildId);

        EmbedBuilder cancel = new EmbedBuilder();
        cancel.setColor(Color.GREEN);
        cancel.setDescription("Успешно отменили Giveaway!");

        event.replyEmbeds(cancel.build())
                .setEphemeral(true)
                .queue();
    }
}
