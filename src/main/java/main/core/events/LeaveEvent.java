package main.core.events;

import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeaveEvent {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;

    @Autowired
    public LeaveEvent(ActiveGiveawayRepository activeGiveawayRepository, LanguageRepository languageRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.languageRepository = languageRepository;
    }

    public void leave(@NotNull GuildLeaveEvent event) {
        var guildId = event.getGuild().getIdLong();
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            languageRepository.deleteById(guildId);
            activeGiveawayRepository.deleteById(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
