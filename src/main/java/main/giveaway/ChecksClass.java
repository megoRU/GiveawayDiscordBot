package main.giveaway;

import main.config.BotStartConfig;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ChecksClass {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    public ChecksClass(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public static boolean canSendGiveaway(GuildChannel srcChannel) {
        return srcChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_SEND);
    }

    public static boolean canSendGiveaway(GuildChannel dstChannel, SlashCommandInteractionEvent event) {
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

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_EMBED_LINKS)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_EMBED_LINKS`" : ", `Permission.MESSAGE_EMBED_LINKS`");
            bool = false;
        }

        if (!selfMember.hasPermission(dstChannel, Permission.MESSAGE_ADD_REACTION)) {
            stringBuilder.append(stringBuilder.length() == 0 ? "`Permission.MESSAGE_ADD_REACTION`" : ", `Permission.MESSAGE_ADD_REACTION`");
            bool = false;
        }

        if (!bool && event != null) {
            event.reply("Bot don't have in <#" + dstChannel.getId() + ">: \n" + stringBuilder).queue();
        }

        return bool;
    }

    public boolean isGuildDeleted(final long guildId) {
        if (BotStartConfig.jda.getGuildById(guildId) != null) {
            return false;
        } else {
            System.out.println("Бота нет в Guild -> Удаляем Giveaway!");
            activeGiveawayRepository.deleteActiveGiveaways(guildId);
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
            return true;
        }
    }
}
