package main.giveaway;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface ChecksClass {

    JSONParsers jsonParsers = new JSONParsers();

    static boolean canSendGiveaway(GuildChannel srcChannel) {
        return srcChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_SEND);
    }

    static boolean canSendGiveaway(GuildChannel dstChannel, SlashCommandInteractionEvent event) {
        Member selfMember = dstChannel.getGuild().getSelfMember();
        StringBuilder stringBuilder = new StringBuilder();

        boolean bool = true;

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_SEND)) {
            stringBuilder.append("`Permission.MESSAGE_SEND`");
            bool = false;
        }

        if (!selfMember.hasPermission(dstChannel, Permission.VIEW_CHANNEL)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.VIEW_CHANNEL`" : ", `Permission.VIEW_CHANNEL`");
            bool = false;
        }

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_HISTORY)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_HISTORY`" : ", `Permission.MESSAGE_HISTORY`");
            bool = false;
        }

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_EMBED_LINKS`" : ", `Permission.MESSAGE_EMBED_LINKS`");
            bool = false;
        }

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_ADD_REACTION)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_ADD_REACTION`" : ", `Permission.MESSAGE_ADD_REACTION`");
            bool = false;
        }

        if (!bool && event != null && event.getGuild() != null) {
            String checkPermissions = String.format(
                    jsonParsers.getLocale("check_permissions", event.getGuild().getId()),
                    dstChannel.getId(),
                    stringBuilder);

            event.reply(checkPermissions).queue();
        }

        return bool;
    }

    //TODO: Я бы лучше переделал это.
    static boolean isGuildDeleted(final long guildId) {
        if (BotStartConfig.getJda().getGuildById(guildId) != null) {
            return false;
        } else {
            System.out.println("Бота нет в Guild -> Удаляем Giveaway!");
            BotStartConfig.getRepositoryHandler().deleteActiveGiveaway(guildId);
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
            return true;
        }
    }
}
