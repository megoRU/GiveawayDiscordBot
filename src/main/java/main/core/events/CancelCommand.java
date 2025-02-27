package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
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
    private static final GiveawayRegistry instance = GiveawayRegistry.getInstance();

    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);

        event.deferReply().setEphemeral(true).queue();

        if (giveawayId != null) {
            if (giveawayId.matches("[0-9]+")) {
                long giveawayIdLong = Long.parseLong(giveawayId);
                Giveaway giveaway = instance.getGiveaway(giveawayIdLong);

                if (giveaway == null) {
                    String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
                    event.getHook().sendMessage(giveawayNotFound).setEphemeral(true).queue();
                } else {
                    removeActiveGiveaway(giveaway);

                    String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);
                    event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
                }
            } else {
                Scheduling scheduling = instance.getScheduling(giveawayId);

                if (scheduling == null) {
                    String selectMenuSchedulingNotFound = jsonParsers.getLocale("select_menu_scheduling_not_found", guildId);
                    event.getHook().sendMessage(selectMenuSchedulingNotFound).setEphemeral(true).queue();
                } else {
                    removeScheduling(scheduling);

                    String cancelSchedulingGiveaway = jsonParsers.getLocale("cancel_scheduling_giveaway", guildId);
                    event.getHook().sendMessage(cancelSchedulingGiveaway).setEphemeral(true).queue();
                }
            }
        } else {
            List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);
            List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);

            if (giveawayList != null && giveawayList.size() > 1) {
                String moreGiveawayForCancel = jsonParsers.getLocale("more_giveaway_for_cancel", guildId);
                event.getHook().sendMessage(moreGiveawayForCancel).setEphemeral(true).queue();
            } else if (schedulingList != null && schedulingList.size() > 1) {
                String moreSchedulingForCancel = jsonParsers.getLocale("more_scheduling_for_cancel", guildId);
                event.getHook().sendMessage(moreSchedulingForCancel).setEphemeral(true).queue();
            } else {
                if (giveawayList != null && giveawayList.size() == 1) {
                    Giveaway giveaway = giveawayList.getFirst();
                    String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);

                    removeActiveGiveaway(giveaway);
                    event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
                } else if (schedulingList != null && schedulingList.size() == 1) {
                    Scheduling scheduling = schedulingList.getFirst();
                    String cancelSchedulingGiveaway = jsonParsers.getLocale("cancel_scheduling_giveaway", guildId);

                    removeScheduling(scheduling);
                    event.getHook().sendMessage(cancelSchedulingGiveaway).setEphemeral(true).queue();
                } else {
                    String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
                    event.getHook().sendMessage(giveawayNotFound).setEphemeral(true).queue();
                }
            }
        }
    }

    private void removeScheduling(@NotNull Scheduling scheduling) {
        String idSalt = scheduling.getIdSalt();
        instance.removeScheduling(idSalt);

        schedulingRepository.deleteByIdSalt(idSalt);
    }

    private void removeActiveGiveaway(@NotNull Giveaway giveaway) {
        long messageId = giveaway.getGiveawayData().getMessageId();
        instance.removeGiveaway(messageId);

        activeGiveawayRepository.deleteByMessageId(messageId);
        giveaway.cancelGiveaway();
    }
}