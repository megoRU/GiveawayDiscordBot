package main.core.events;

import main.service.SlashService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class JoinEvent {

    public void join(@NotNull GuildJoinEvent event) {
        Member selfMember = event.getGuild().getSelfMember();
        EmbedBuilder welcome = new EmbedBuilder();

        Long settings = SlashService.getCommandId("settings");
        Long start = SlashService.getCommandId("start");
        Long reroll = SlashService.getCommandId("reroll");
        Long help = SlashService.getCommandId("help");
        Long stop = SlashService.getCommandId("stop");
        Long predefined = SlashService.getCommandId("predefined");

        welcome.setColor(Color.GREEN);
        welcome.addField("Giveaway", "Thanks for adding " + "**" + "Giveaway" + "** " + "bot to " + event.getGuild().getName() + "!\n", false);
        welcome.addField("Setup Bot Language", String.format("Use: </settings:%s>", settings), false);
        welcome.addField("Create Giveaway", String.format("Use: </start:%s>", start), false);
        welcome.addField("Create predefined Giveaway", String.format("Use: </predefined:%s> (Permission Manage server)", predefined), false);
        welcome.addField("Reroll Winner", String.format("Use: </reroll:%s>", reroll), false);
        welcome.addField("Stop Giveaway manually", String.format("Use: </stop:%s> (Permission Manage server)", stop), false);
        welcome.addField("List of commands", String.format("Use: </help:%s>", help), false);
        welcome.addField("Support server", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)\n", false);
        welcome.addField("Information", "Our bot supports recovery of any Giveaway, upon request in support. " +
                "Also, the bot automatically checks the lists of participants, even if the bot is turned off or there are problems in recording while working, " +
                "it will automatically restore everything. This gives a 100% guarantee that each participant will be recorded.", false);

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

        DefaultGuildChannelUnion defaultChannel = event.getGuild().getDefaultChannel();

        if (defaultChannel != null) {
            if (defaultChannel.getType() == ChannelType.TEXT) {
                TextChannel textChannel = defaultChannel.asTextChannel();
                if (selfMember.hasPermission(textChannel, Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_EMBED_LINKS)) {
                    textChannel.sendMessageEmbeds(welcome.build()).setActionRow(buttons).queue();
                }
            }
        }
    }
}