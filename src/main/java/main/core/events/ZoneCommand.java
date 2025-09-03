package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.entity.UserZoneId;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.model.repository.UserZoneIdRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ZoneCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final UserZoneIdRepository userZoneIdRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    public void update(@NotNull SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();

        event.deferReply().setEphemeral(true).queue();

        String zone = event.getOption("zone", OptionMapping::getAsString);

        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findActiveGiveawaysByCreatedUserId(userId);
        List<Scheduling> schedulingList = schedulingRepository.findSchedulingByCreatedUserId(userId);

        if (!activeGiveawaysList.isEmpty() || !schedulingList.isEmpty()) {
            String zoneError = String.format(jsonParsers.getLocale("zone_error", guildId), zone);
            event.getHook().sendMessage(zoneError).setEphemeral(true).queue();
            return;
        }

        if (zone != null && zone.matches("[A-Za-z]+(/[A-Za-z_]+)+|UTC")) {
            UserZoneId userZoneId = userZoneIdRepository.findByUserId(userId);
            if (userZoneId == null) {
                userZoneId = new UserZoneId();
                userZoneId.setUserId(userId);
            }

            userZoneId.setZoneId(zone);
            userZoneIdRepository.save(userZoneId);

            BotStart.setTimeZone(userId, zone);

            String zoneEdit = String.format(jsonParsers.getLocale("zone_edit", guildId), zone);
            event.getHook().sendMessage(zoneEdit).setEphemeral(true).queue();
        } else {
            String slashErrors = String.format(jsonParsers.getLocale("slash_errors", guildId), zone);
            event.getHook().sendMessage(slashErrors).setEphemeral(true).queue();
        }
    }
}