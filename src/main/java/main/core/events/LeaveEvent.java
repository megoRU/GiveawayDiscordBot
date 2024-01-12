package main.core.events;

import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SettingsRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveEvent {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SettingsRepository settingsRepository;

    @Autowired
    public LeaveEvent(ActiveGiveawayRepository activeGiveawayRepository, SettingsRepository settingsRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.settingsRepository = settingsRepository;
    }

    public void leave(@NotNull GuildLeaveEvent event) {
        var guildId = event.getGuild().getIdLong();
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            settingsRepository.deleteById(guildId);
            activeGiveawayRepository.deleteById(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
