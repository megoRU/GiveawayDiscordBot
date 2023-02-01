package main.core.events;

import main.config.BotStart;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;

@Service
public class LanguageCommand {

    private final LanguageRepository languageRepository;

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public LanguageCommand(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    public void language(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getId();

        BotStart.getMapLanguages().put(guildId, event.getOptions().get(0).getAsString());

        String lang = event.getOptions().get(0).getAsString().equals("rus") ? "Русский" : "English";
        String buttonLanguage = String.format(jsonParsers.getLocale("button_language", guildId), lang);

        EmbedBuilder button = new EmbedBuilder();
        button.setColor(Color.GREEN);
        button.setDescription(buttonLanguage);

        event.replyEmbeds(button.build()).setEphemeral(true).queue();

        Language language = new Language();
        language.setServerId(guildId);
        language.setLanguage(event.getOptions().get(0).getAsString());
        languageRepository.save(language);
    }
}
