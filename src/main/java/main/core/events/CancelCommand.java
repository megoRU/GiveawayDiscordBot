package main.core.events;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CancelCommand {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final SchedulingRepository schedulingRepository;
    private final UpdateController updateController;
    private static final JSONParsers jsonParsers = new JSONParsers();

    @Transactional
    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);

        event.deferReply().setEphemeral(true).queue();

        if (giveawayId != null) {
            if (giveawayId.matches("[0-9]+")) {
                long giveawayIdLong = Long.parseLong(giveawayId);
                ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByMessageId(giveawayIdLong);

                if (activeGiveaways == null || !activeGiveaways.getGuildId().equals(guildId)) {
                    String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
                    event.getHook().sendMessage(giveawayNotFound).setEphemeral(true).queue();
                } else {
                    removeActiveGiveaway(activeGiveaways);

                    String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);
                    event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
                }
            } else {
                Scheduling scheduling = schedulingRepository.findByIdSalt(giveawayId);

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
            List<ActiveGiveaways> giveawayList = activeGiveawayRepository.findByGuildId(guildId);
            List<Scheduling> schedulingList = schedulingRepository.findByGuildId(guildId);

            if (giveawayList != null && giveawayList.size() > 1) {
                String moreGiveawayForCancel = jsonParsers.getLocale("more_giveaway_for_cancel", guildId);
                event.getHook().sendMessage(moreGiveawayForCancel).setEphemeral(true).queue();
            } else if (schedulingList.size() > 1) {
                String moreSchedulingForCancel = jsonParsers.getLocale("more_scheduling_for_cancel", guildId);
                event.getHook().sendMessage(moreSchedulingForCancel).setEphemeral(true).queue();
            } else {
                if (giveawayList != null && giveawayList.size() == 1) {
                    ActiveGiveaways activeGiveaways = giveawayList.getFirst();
                    String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);

                    removeActiveGiveaway(activeGiveaways);
                    event.getHook().sendMessage(cancelGiveaway).setEphemeral(true).queue();
                } else if (schedulingList.size() == 1) {
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
        schedulingRepository.deleteByIdSalt(idSalt);
    }

    private void removeActiveGiveaway(@NotNull ActiveGiveaways activeGiveaways) {
        Set<Long> participantsList = activeGiveaways.getParticipants() != null ? activeGiveaways.getParticipants()
                .stream()
                .map(Participants::getUserId)
                .collect(Collectors.toSet()) : Collections.emptySet();

        Giveaway giveaway = new Giveaway(
                activeGiveaways.getGuildId(),
                activeGiveaways.getChannelId(),
                activeGiveaways.getCreatedUserId(),
                activeGiveaways.isFinish(),
                false,
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
        giveaway.cancelGiveaway();
    }
}