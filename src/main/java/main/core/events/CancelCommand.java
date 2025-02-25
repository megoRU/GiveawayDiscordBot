package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CancelCommand {

    private final SchedulingRepository schedulingRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);

        event.deferReply().setEphemeral(true).queue();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        if (giveawayId != null) {
            if (giveawayId.matches("[0-9]+")) {
                long giveawayIdLong = Long.parseLong(giveawayId);
                Giveaway giveaway = instance.getGiveaway(giveawayIdLong);

                if (giveaway == null) {
                    String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
                    event.getHook().sendMessage(giveawayNotFound).setEphemeral(true).queue();
                } else {
                    removeActiveGiveaway(giveawayIdLong);

                    String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);
                    event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
                }
            } else {
                Scheduling scheduling = instance.getScheduling(giveawayId);

                if (scheduling == null) {
                    String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
                    event.getHook().sendMessage(giveawayNotFound).setEphemeral(true).queue();
                } else {
                    removeScheduling(giveawayId);

                    String cancelSchedulingGiveaway = jsonParsers.getLocale("cancel_scheduling_giveaway", guildId);
                    event.getHook().sendMessage(cancelSchedulingGiveaway).setEphemeral(true).queue();
                }
            }
        } else {
            List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findByGuildId(guildId);

            if (activeGiveawaysList != null && activeGiveawaysList.size() > 1) {
                String moreGiveawayForCancel = jsonParsers.getLocale("more_giveaway_for_cancel", guildId);
                event.getHook().sendMessage(moreGiveawayForCancel).setEphemeral(true).queue();
            } else if (activeGiveawaysList != null && activeGiveawaysList.size() == 1) {
                ActiveGiveaways first = activeGiveawaysList.getFirst();
                String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);

                removeActiveGiveaway(first.getMessageId());

                event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
            } else {
                String noActiveGiveawayUseParameter = jsonParsers.getLocale("no_active_giveaway_use_parameter", guildId);
                event.getHook().sendMessage(noActiveGiveawayUseParameter).setEphemeral(true).queue();
            }
        }
    }

    private void removeScheduling(String giveawayId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeScheduling(giveawayId);

        schedulingRepository.deleteByIdSalt(giveawayId);
    }

    private void removeActiveGiveaway(long messageId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeGiveaway(messageId);

        activeGiveawayRepository.deleteByMessageId(messageId);
    }
}