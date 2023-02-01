package main.core.events;

import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ButtonChangeLanguage {

    private final LanguageRepository languageRepository;

    public static final String CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private static final JSONParsers jsonParsers = new JSONParsers();
    private static final String FLAG_RUS = "\uD83C\uDDF7\uD83C\uDDFA"; //🇷🇺

    @Autowired
    public ButtonChangeLanguage(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    public void change(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        if (event.getButton().getEmoji() != null) {
            String buttonName = event.getButton().getEmoji().getName().contains(FLAG_RUS) ? "rus" : "eng";
            event.editButton(event.getButton().asDisabled()).queue();

            Language language = new Language();
            language.setServerId(Objects.requireNonNull(event.getGuild()).getId());
            language.setLanguage(buttonName);
            languageRepository.save(language);

            BotStart.getMapLanguages().put(event.getGuild().getId(), buttonName);
            String buttonLanguage = String.format(jsonParsers.getLocale("button_language", event.getGuild().getId()), buttonName);

            event.getHook().sendMessage(buttonLanguage).setEphemeral(true).queue();
        }
    }
}
