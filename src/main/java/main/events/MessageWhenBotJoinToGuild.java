package main.events;

import main.config.BotStartConfig;
import main.config.RepositoryHandler;
import main.giveaway.buttons.ReactionsButton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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

@Service
public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private static final RepositoryHandler repositoryHandler = BotStartConfig.getRepositoryHandler();

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() == null
                    || !event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_SEND)
                    || !event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_EMBED_LINKS)) {
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
            welcome.addField("Information", "We are actively writing about new updates or problems in our discord. We recommend that you follow up.", false);
            welcome.addField("One more Thing", "If you are not satisfied with something in the bot, please let us know, we will fix it!", false);

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

            event.getGuild()
                    .getDefaultChannel()
                    .asTextChannel()
                    .sendMessageEmbeds(welcome.build())
                    .setActionRow(buttons)
                    .queue();
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

            repositoryHandler.deleteLanguage(event.getGuild().getId());
            repositoryHandler.deleteActiveGiveaway(event.getGuild().getIdLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}