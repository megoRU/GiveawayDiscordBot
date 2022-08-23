package main.giveaway.buttons;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.Language;
import main.model.entity.Notification;
import main.model.repository.LanguageRepository;
import main.model.repository.NotificationRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;

@AllArgsConstructor
@Service
public class ReactionsButton extends ListenerAdapter implements SenderMessage {

    public static final String CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    public static final String DISABLE_NOTIFICATIONS = "DISABLE_NOTIFICATIONS";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final NotificationRepository notificationRepository;
    private static final String FLAG_RUS = "\uD83C\uDDF7\uD83C\uDDFA"; //🇷🇺

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() != null && Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + CHANGE_LANGUAGE)) {
            event.deferEdit().queue();
            if (event.getButton().getEmoji() != null) {
                String buttonName = event.getButton().getEmoji().getName().contains(FLAG_RUS) ? "rus" : "eng";

                Language language = new Language();
                language.setServerId(event.getGuild().getId());
                language.setLanguage(buttonName);
                languageRepository.save(language);

                BotStartConfig.getMapLanguages().put(event.getGuild().getId(), buttonName);

                String buttonLanguage = String.format(jsonParsers.getLocale("button_language", event.getGuild().getId()), buttonName);

                event.getHook()
                        .sendMessage(buttonLanguage)
                        .setEphemeral(true).queue();
            }
            return;
        }

        if (Objects.equals(event.getButton().getId(), DISABLE_NOTIFICATIONS)) {
            event.deferEdit().queue();

            Notification notification = new Notification();
            notification.setUserIdLong(event.getUser().getId());
            notification.setNotificationStatus(Notification.NotificationStatus.DENY);
            BotStartConfig.getMapNotifications().put(event.getUser().getId(), Notification.NotificationStatus.DENY);

            //Не перевести на русский ибо у нас другая реализация (Only Guild)
            event.getHook()
                    .sendMessage("Now the bot will not notify you!")
                    .queue();
            notificationRepository.save(notification);
        }
    }
}
