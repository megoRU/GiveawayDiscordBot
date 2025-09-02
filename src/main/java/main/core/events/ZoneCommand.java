package main.core.events;

import lombok.AllArgsConstructor;
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
        final String zoneFinal = event.getOption("zone", OptionMapping::getAsString);

        if (zone != null && zoneFinal != null && zone.matches("^[+-][0-9]{1,2}$")) {
            // Преобразуем однозначные числа в двухзначные
            String sign = zone.substring(0, 1);
            int number = Integer.parseInt(zone.substring(1));
            if (number >= 0 && number <= 9) {
                zone = sign + String.format("%02d", number);
            }

            System.out.println(zone);

            UserZoneId userZoneId = userZoneIdRepository.findByUserId(userId);
            if (userZoneId == null) {
                userZoneId = new UserZoneId();
                userZoneId.setUserId(userId);
            }

            userZoneId.setZoneId(zone);
            userZoneIdRepository.save(userZoneId);

            String zoneEdit = String.format(jsonParsers.getLocale("zone_edit", guildId), zoneFinal);
            event.reply(zoneEdit).queue();
        } else {
            String slashErrors = String.format(jsonParsers.getLocale("slash_errors", guildId), zone);
            event.reply(slashErrors).queue();
        }
    }
}