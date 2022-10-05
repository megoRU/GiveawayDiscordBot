package main.events;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.giveaway.buttons.ReactionsButton;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() == null ||
                    !event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(),
                                    Permission.MESSAGE_SEND,
                                    Permission.MESSAGE_EMBED_LINKS,
                                    Permission.VIEW_CHANNEL)) {
                return;
            }

            EmbedBuilder welcome = new EmbedBuilder();
            welcome.setColor(Color.GREEN);
            welcome.addField("Giveaway", "Thanks for adding " +
                    "**"
                    + event.getGuild().getSelfMember().getUser().getName() +
                    "** " + "bot to " + event.getGuild().getName() +
                    "!\n", false);
            welcome.addField("List of commands", "Use Slash Command: **/help** for a list of commands.", false);
            welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
            welcome.addField("Information", "Our bot supports recovery of any Giveaway, upon request in support. " +
                    "Also, the bot automatically checks the lists of participants, even if the bot is turned off or there are problems in recording while working, " +
                    "it will automatically restore everything. This gives a 100% guarantee that each participant will be recorded.", false);

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
            buttons.add(Button.link("https://patreon.com/ghbots", "Patreon"));

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

            DefaultGuildChannelUnion defaultChannel = event.getGuild().getDefaultChannel();
            if (defaultChannel instanceof TextChannel) {
                defaultChannel
                        .asTextChannel()
                        .sendMessageEmbeds(welcome.build())
                        .setActionRow(buttons)
                        .queue();
            }
        } catch (Exception e) {
            System.out.println("Скорее всего нет `DefaultChannel`!");
            e.printStackTrace();
        }
    }

    //TODO: Сделать в таблице ON DELETE CASCADE
    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            languageRepository.deleteLanguage(event.getGuild().getId());
            activeGiveawayRepository.deleteActiveGiveaways(event.getGuild().getIdLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}