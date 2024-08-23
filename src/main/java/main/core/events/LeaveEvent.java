package main.core.events;

import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveEvent {

    private final static Logger LOGGER = LoggerFactory.getLogger(LeaveEvent.class.getName());
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    @Autowired
    public LeaveEvent(ActiveGiveawayRepository activeGiveawayRepository,
                      SchedulingRepository schedulingRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.schedulingRepository = schedulingRepository;
    }

    public void leave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            activeGiveawayRepository.deleteById(event.getGuild().getIdLong());
            schedulingRepository.deleteById(event.getGuild().getIdLong());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
