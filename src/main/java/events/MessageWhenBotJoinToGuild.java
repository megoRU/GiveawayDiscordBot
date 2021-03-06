package events;

import db.DataBase;
import giveaway.ReactionsButton;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MessageWhenBotJoinToGuild extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();

    //bot join msg
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {

        try {
            if (event.getGuild().getDefaultChannel() != null &&
                    !event.getGuild().getSelfMember().hasPermission(event.getGuild().getDefaultChannel(),
                            Permission.MESSAGE_WRITE)) {
                return;
            }
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

            event.getGuild().getDefaultChannel().sendMessageEmbeds(welcome.build())
                    .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP,
                            jsonParsers.getLocale("button_Help", event.getGuild().getId())))
                    .queue();
            welcome.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        try {
            System.out.println("Удаляем данные после удаления бота из Guild");
            DataBase.getInstance().removeLangFromDB(event.getGuild().getId());
            DataBase.getInstance().removePrefixFromDB(event.getGuild().getId());
            DataBase.getInstance().removeMessageFromDB(event.getGuild().getIdLong());
            DataBase.getInstance().dropTableWhenGiveawayStop(event.getGuild().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}