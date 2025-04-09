package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Settings;
import main.model.repository.SettingsRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@AllArgsConstructor
public class EndMessageCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final SettingsRepository settingsRepository;

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        var text = event.getOption("text", OptionMapping::getAsString);
        var userId = event.getUser().getIdLong();
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (text != null && text.contains("@winner")) {
            String endMessage = jsonParsers.getLocale("end_message", guildId);

            if (!text.contains("@link")) text = text.concat(" \n@link");

            updateSettings(guildId, text);

            long latestMessageId = event.getMessageChannel().getLatestMessageIdLong();
            long messageIdLong = event.getMessageChannel().getIdLong();
            String url = GiveawayUtils.getDiscordUrlMessage(guildId, messageIdLong, latestMessageId);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);

            String string = text
                    .replaceAll("@winner", "<@" + userId + ">")
                    .replaceAll("@link", giftUrl);

            event.reply(String.format("""
                            %s
                            
                            %s
                            """, endMessage, string))
                    .setEphemeral(true)
                    .queue();
        } else if (text == null) {
            updateSettings(guildId, null);
            String endMessageDeleted = jsonParsers.getLocale("end_message_deleted", guildId);
            event.reply(endMessageDeleted).setEphemeral(true).queue();
        } else {
            String endMessageEditError = jsonParsers.getLocale("end_message_edit_error", guildId);
            event.reply(endMessageEditError).setEphemeral(true).queue();
        }
    }

    private void updateSettings(long guildId, @Nullable String text) {
        Map<Long, Settings> mapLanguages = BotStart.getMapLanguages();
        Settings settings = mapLanguages.get(guildId);

        if (settings == null) {
            settings = new Settings();
            settings.setServerId(guildId);
            settings.setLanguage("eng");
        }

        settings.setText(text);

        BotStart.getMapLanguages().put(guildId, settings);
        settingsRepository.save(settings);
    }
}