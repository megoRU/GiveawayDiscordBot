package main.messagesevents;

import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import main.config.BotStartConfig;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class LanguageChange extends ListenerAdapter {

    private static final String LANG_RUS = "!lang rus";
    private static final String LANG_ENG = "!lang eng";
    private static final String LANG_RESET = "!lang reset";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.TEXT)) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_SEND)) return;

        if (event.getMember() == null) return;

        String message = event.getMessage().getContentRaw().toLowerCase().trim();
        String[] messages = message.split(" ", 2);
        String prefix_LANG_RUS = LANG_RUS;
        String prefix_LANG_ENG = LANG_ENG;
        String prefix_LANG_RESET = LANG_RESET;

        if (BotStartConfig.getMapPrefix().containsKey(event.getGuild().getId())) {
            prefix_LANG_RUS = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "lang rus";
            prefix_LANG_ENG = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "lang eng";
            prefix_LANG_RESET = BotStartConfig.getMapPrefix().get(event.getGuild().getId()) + "lang reset";
        }

        if ((message.equals(prefix_LANG_RUS)
                || message.equals(prefix_LANG_RESET)
                || message.equals(prefix_LANG_ENG))
                && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("language_change_Not_Admin", event.getGuild().getId()))
                    .queue();
            return;
        }

        if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), messages[1]);

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage( messages[1]);
            languageRepository.save(language);

            event.getChannel()
                    .sendMessage(jsonParsers
                            .getLocale("language_change_lang", event.getGuild().getId())
                            + "`" + messages[1].toUpperCase() + "`")
                    .queue();

            return;
        }

        if (message.equals(prefix_LANG_RESET)) {
            BotStartConfig.getMapLanguages().remove(event.getGuild().getId());
            languageRepository.deleteLanguage(event.getGuild().getId());
            event.getChannel()
                    .sendMessage(jsonParsers.getLocale("language_change_lang_reset", event.getGuild().getId()))
                    .queue();
        }
    }
}
