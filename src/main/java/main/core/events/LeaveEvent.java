package main.core.events;

import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveEvent {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    @Autowired
    public LeaveEvent(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void leave(@NotNull GuildLeaveEvent event) {
        var guildId = event.getGuild().getIdLong();
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            activeGiveawayRepository.deleteById(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
