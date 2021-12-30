package main.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.giveaway.ReactionsButton;
import main.jsonparser.JSONParsers;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.PrefixRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final PrefixRepository prefixRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() != null &&
                    !event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_SEND)) {
                return;
            }

            if (event.getGuild().getDefaultChannel() != null) {

                EmbedBuilder welcome = new EmbedBuilder();
                welcome.setColor(Color.GREEN);
                welcome.addField("Giveaway", "Thanks for adding " +
                        "**"
                        + event.getGuild().getSelfMember().getUser().getName() +
                        "** " + "bot to " + event.getGuild().getName() +
                        "!\n", false);
                welcome.addField("List of commands", "Use **!help** for a list of commands.", false);
                welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
                welcome.addField("One more Thing", "If you are not satisfied with something in the bot, please let us know, we will fix it!"
                        , false);

                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP, jsonParsers.getLocale("button_Help", event.getGuild().getId())));
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));


                if (BotStartConfig.getMapLanguages().get(event.getGuild().getId()) != null) {

                    if (BotStartConfig.getMapLanguages().get(event.getGuild().getId()).equals("eng")) {

                        buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                        "Сменить язык ")
                                .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                    } else {
                        buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                        "Change language ")
                                .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                    }
                } else {
                    buttons.add(Button.secondary(event.getGuild().getId() + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                    "Сменить язык ")
                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                }


                event.getGuild().getDefaultChannel().sendMessageEmbeds(welcome.build())
                        .setActionRow(buttons).queue();

                welcome.clear();

            }
        } catch (Exception e) {
            System.out.println("Скорее всего нет `DefaultChannel`!");
            e.printStackTrace();
        }

        try {

            List<OptionData> optionsLanguage = new ArrayList<>();
            List<OptionData> optionsStart = new ArrayList<>();
            List<OptionData> optionsStop = new ArrayList<>();

            optionsLanguage.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
                    .addChoice("eng", "eng")
                    .addChoice("rus", "rus")
                    .setRequired(true));

            optionsStart.add(new OptionData(OptionType.STRING, "title", "Title for Giveaway")
                    .setName("title")
            );

            optionsStart.add(new OptionData(OptionType.INTEGER, "count", "Set count winners")
                    .setName("count")
            );

            optionsStart.add(new OptionData(OptionType.STRING, "duration", "Examples: 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style. Preferably immediately in UTC ±0")
                    .setName("duration")
            );

            optionsStart.add(new OptionData(OptionType.CHANNEL, "channel", "#text channel name")
                    .setName("channel")
            );

            optionsStop.add(new OptionData(OptionType.STRING, "stop", "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1")
                    .setName("stop")
            );


            event.getGuild().upsertCommand("language", "Setting language").addOptions(optionsLanguage).queue();
            event.getGuild().upsertCommand("giveaway-start", "Create giveaway").addOptions(optionsStart).queue();
            event.getGuild().upsertCommand("giveaway-stop", "Stop the Giveaway").addOptions(optionsStop).queue();
            event.getGuild().upsertCommand("help", "Bot commands").queue();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            languageRepository.deleteLanguage(event.getGuild().getId());
            prefixRepository.deletePrefix(event.getGuild().getId());
            activeGiveawayRepository.deleteActiveGiveaways(event.getGuild().getIdLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}