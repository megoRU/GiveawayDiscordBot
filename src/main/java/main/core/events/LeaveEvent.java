package main.core.events;

import lombok.AllArgsConstructor;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LeaveEvent {

    private final static Logger LOGGER = LoggerFactory.getLogger(LeaveEvent.class.getName());
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    public void leave(@NotNull GuildLeaveEvent event) {
        try {
            long guildId = event.getGuild().getIdLong();
            System.out.println("Удаляем данные после удаления бота из Guild");
            activeGiveawayRepository.deleteAllByGuildId(guildId);
            schedulingRepository.deleteById(guildId);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
