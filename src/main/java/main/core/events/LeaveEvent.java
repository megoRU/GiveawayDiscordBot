package main.core.events;

import lombok.AllArgsConstructor;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LeaveEvent {

    private final static Logger LOGGER = LoggerFactory.getLogger(LeaveEvent.class.getName());
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    public void leave(@NotNull GuildLeaveEvent event) {
        try {
            LOGGER.info("Leaving guild {}", event.getGuild().getName());
            long guildId = event.getGuild().getIdLong();
            activeGiveawayRepository.deleteAllByGuildId(guildId);
            schedulingRepository.deleteByGuildId(guildId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}