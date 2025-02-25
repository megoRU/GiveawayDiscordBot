package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
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
    private final static Logger LOGGER = LoggerFactory.getLogger(DeleteEvent.class.getName());

    public void handle(@NotNull MessageDeleteEvent event) {
        var messageId = event.getMessageIdLong();
        boolean fromGuild = event.isFromGuild();

        if (fromGuild) {
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Giveaway giveaway = instance.getGiveaway(messageId);

            if (giveaway != null) {
                if (giveaway.getGiveawayData().getMessageId() == messageId) {
                    giveawayRepositoryService.deleteGiveaway(messageId);
                    LOGGER.info("DeleteEvent: {}", messageId);
                }
            }
        }
    }
}