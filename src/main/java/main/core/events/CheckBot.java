package main.core.events;

import main.giveaway.utils.ChecksClass;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class CheckBot {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void check(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();

        GuildChannel guildChannel = event.getOption("textchannel", OptionMapping::getAsChannel);
        if (guildChannel == null) {
            event.reply("Channel is Null").queue();
        } else {
            boolean canSendGiveaway = ChecksClass.canSendGiveaway(guildChannel, event);
            if (canSendGiveaway) {
                String giftPermissions = String.format(jsonParsers.getLocale("gift_permissions", guildId), guildChannel.getId());
                event.reply(giftPermissions).queue();
            }
        }
    }
}