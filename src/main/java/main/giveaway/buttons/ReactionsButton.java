package main.giveaway.buttons;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
import main.model.entity.Language;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.logging.Logger;

@AllArgsConstructor
@Service
public class ReactionsButton extends ListenerAdapter implements SenderMessage {

    public static final String BUTTON_EXAMPLES = "BUTTON_EXAMPLES";
    public static final String BUTTON_HELP = "BUTTON_HELP";
    public static final String CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private final static Logger LOGGER = Logger.getLogger(ReactionsButton.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) return;

        if (event.getUser().isBot()) return;

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_EXAMPLES)) {

            event.deferEdit().queue();
            event.getChannel().sendMessage(jsonParsers
                            .getLocale("message_gift_Not_Correct_For_Button", event.getGuild().getId())
                            .replaceAll("\\{0}",
                                    BotStartConfig.getMapPrefix().get(event.getGuild().getId())
                                            == null
                                            ? "!"
                                            : BotStartConfig.getMapPrefix().get(event.getGuild().getId())))
                    .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP,
                            jsonParsers.getLocale("button_Help", event.getGuild().getId())))
                    .queue();
            return;
        }

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + CHANGE_LANGUAGE)) {
            event.deferEdit().queue();
            String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage(buttonName);
            languageRepository.save(language);

            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), buttonName);

            event.getHook().sendMessage(jsonParsers
                            .getLocale("button_Language", event.getGuild().getId())
                            .replaceAll("\\{0}", buttonName))
                    .setEphemeral(true).queue();
        }
    }
}
