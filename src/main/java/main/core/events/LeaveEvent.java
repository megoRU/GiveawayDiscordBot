package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.model.entity.Settings;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.model.repository.SettingsRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class LeaveEvent {

    private final static Logger LOGGER = LoggerFactory.getLogger(LeaveEvent.class.getName());
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;

    public void leave(@NotNull GuildLeaveEvent event) {
        try {
            LOGGER.info("Leaving guild {}", event.getGuild().getName());
            long guildId = event.getGuild().getIdLong();
            activeGiveawayRepository.deleteAllByGuildId(guildId);
            schedulingRepository.deleteByGuildId(guildId);
            settingsRepository.deleteByServerId(guildId);

            Map<Long, Settings> mapLanguages = BotStart.getMapLanguages();
            mapLanguages.remove(guildId);

            BotStart.updateActivity();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}