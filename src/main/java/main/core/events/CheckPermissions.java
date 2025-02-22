package main.core.events;

import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CheckPermissions {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void check(@NotNull SlashCommandInteractionEvent event) {
        GuildChannelUnion textChannel = event.getOption("text-channel", OptionMapping::getAsChannel);
        GuildChannel guildChannel = textChannel != null ? textChannel : event.getGuildChannel();

        checkPermissions(guildChannel, event);
    }

    public void checkPermissions(GuildChannel textChannel, @NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        Member selfMember = event.getGuild().getSelfMember();

        StringBuilder stringBuilder = new StringBuilder();

        if (!selfMember.hasPermission(textChannel, Permission.MESSAGE_SEND)) {
            stringBuilder.append("`Permission.MESSAGE_SEND`");
        }

        if (!selfMember.hasPermission(textChannel, Permission.VIEW_CHANNEL)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.VIEW_CHANNEL`" : ",\n`Permission.VIEW_CHANNEL`");
        }

        if (!selfMember.hasPermission(textChannel, Permission.MESSAGE_HISTORY)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_HISTORY`" : ",\n`Permission.MESSAGE_HISTORY`");
        }

        if (!selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_EMBED_LINKS`" : ",\n`Permission.MESSAGE_EMBED_LINKS`");
        }

        if (!selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION)) {
            stringBuilder.append(stringBuilder.isEmpty() ? "`Permission.MESSAGE_ADD_REACTION`" : ",\n`Permission.MESSAGE_ADD_REACTION`");
        }

        if (stringBuilder.isEmpty()) {
            String giftPermissions = String.format(jsonParsers.getLocale("gift_permissions", guildId), textChannel.getId());
            event.reply(giftPermissions).queue();
        } else {
            String checkPermissions = jsonParsers.getLocale("check_permissions", guildId);
            event.reply(String.format(checkPermissions, textChannel.getId(), stringBuilder)).queue();
        }
    }
}