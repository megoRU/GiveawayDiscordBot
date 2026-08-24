package main.core.events;

import lombok.AllArgsConstructor;
import main.model.entity.ActiveGiveaways;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DeleteEvent {

    private final GiveawayRepositoryService giveawayRepositoryService;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteEvent.class.getName());

    public void handle(@NotNull MessageDeleteEvent event) {
        var messageId = event.getMessageIdLong();
        boolean fromGuild = event.isFromGuild();

        if (fromGuild) {
            ActiveGiveaways giveaway = giveawayRepositoryService.getGiveaway(messageId);

            if (giveaway != null) {
                if (giveaway.getMessageId() == messageId) {
                    giveawayRepositoryService.deleteGiveaway(messageId);
                    LOGGER.info("DeleteEvent: {}", messageId);
                }
            }
        }
    }
}