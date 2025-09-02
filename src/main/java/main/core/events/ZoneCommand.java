package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.UserZoneId;
import main.model.repository.UserZoneIdRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final UserZoneIdRepository userZoneIdRepository;

    public void update(@NotNull SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();

        String zone = event.getOption("zone", OptionMapping::getAsString);

        if (zone != null && zone.matches("[A-Za-z]+(/[A-Za-z_]+)+")) {
            UserZoneId userZoneId = userZoneIdRepository.findByUserId(userId);
            if (userZoneId == null) {
                userZoneId = new UserZoneId();
                userZoneId.setUserId(userId);
            }

            userZoneId.setZoneId(zone);
            userZoneIdRepository.save(userZoneId);

            BotStart.setTimeZone(userId, zone);

            String zoneEdit = String.format(jsonParsers.getLocale("zone_edit", guildId), zone);
            event.reply(zoneEdit).queue();
        } else {
            String slashErrors = String.format(jsonParsers.getLocale("slash_errors", guildId), zone);
            event.reply(slashErrors).queue();
        }
    }
}