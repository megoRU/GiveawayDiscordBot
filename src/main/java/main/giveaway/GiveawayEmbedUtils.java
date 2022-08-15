package main.giveaway;

import main.giveaway.impl.GiftHelper;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;
import java.util.logging.Logger;

public class GiveawayEmbedUtils {

    private static final Logger LOGGER = Logger.getLogger(GiveawayEmbedUtils.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    public static EmbedBuilder embedBuilder(final String winners, final int countWinner, final long guildIdLong) {
        String idUserWhoCreateGiveaway = GiveawayRegistry.getInstance().getIdUserWhoCreateGiveaway(guildIdLong);
        EmbedBuilder embedBuilder = new EmbedBuilder();

        LOGGER.info("\nEmbedBuilder: " + winners + " " +  countWinner + " " + guildIdLong);
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setTitle(GiveawayRegistry.getInstance().getTitle(guildIdLong));

        if (countWinner == 1) {
            embedBuilder.appendDescription(jsonParsers.getLocale("gift_winner", String.valueOf(guildIdLong)) + winners);
        } else {
            embedBuilder.appendDescription(jsonParsers.getLocale("gift_winners", String.valueOf(guildIdLong)) + winners);
        }

        String footer = GiftHelper.setEndingWord(countWinner, guildIdLong);
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(countWinner + " " + footer + " | " + jsonParsers.getLocale("gift_Ends", String.valueOf(guildIdLong)));

        if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildIdLong)) {
            embedBuilder.appendDescription(jsonParsers.getLocale("gift_OnlyFor", String.valueOf(guildIdLong))
                    + " <@&" + GiveawayRegistry.getInstance().getRoleId(guildIdLong) + ">");
        }

        embedBuilder.appendDescription("\nHosted by: " + "<@" + idUserWhoCreateGiveaway + ">");
        embedBuilder.appendDescription("\nGiveaway ID: `" + (guildIdLong + Long.parseLong(GiveawayRegistry.getInstance().getMessageId(guildIdLong))) + "`");

        if (GiveawayRegistry.getInstance().getUrlImage(guildIdLong) != null) {
            embedBuilder.setImage(GiveawayRegistry.getInstance().getUrlImage(guildIdLong));
        }

        return embedBuilder;
    }
}