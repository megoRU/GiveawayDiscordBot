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
        long idUserWhoCreateGiveaway = GiveawayRegistry.getInstance().getIdUserWhoCreateGiveaway(guildIdLong);
        EmbedBuilder embedBuilder = new EmbedBuilder();

        LOGGER.info("\nEmbedBuilder: " +
                "\nwinners: " + winners +
                "\ncountWinner: " + countWinner
                +"\nguildIdLong: " + guildIdLong);

        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setTitle(GiveawayRegistry.getInstance().getTitle(guildIdLong));

        if (countWinner == 1) {
            String giftWinner = String.format(jsonParsers.getLocale("gift_winner", String.valueOf(guildIdLong)), winners);
            embedBuilder.appendDescription(giftWinner);
        } else {
            String giftWinners = String.format(jsonParsers.getLocale("gift_winners", String.valueOf(guildIdLong)), winners);
            embedBuilder.appendDescription(giftWinners);
        }

        String footer = countWinner + " " + GiftHelper.setEndingWord(countWinner, guildIdLong);
        embedBuilder.setTimestamp(Instant.now());
        String giftEnds = String.format(jsonParsers.getLocale("gift_ends", String.valueOf(guildIdLong)), footer);
        embedBuilder.setFooter(giftEnds);

        if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildIdLong)) {
            Long roleId = GiveawayRegistry.getInstance().getRoleId(guildIdLong);
            String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", String.valueOf(guildIdLong)), roleId);

            embedBuilder.appendDescription(giftOnlyFor);
        }
        long giveawayIdLong = GiveawayRegistry.getInstance().getMessageId(guildIdLong);

        String hostedBy = String.format("\nHosted by: <@%s>", idUserWhoCreateGiveaway);
        String giveawayIdDescription = String.format("\nGiveaway ID: `%s`", giveawayIdLong);

        embedBuilder.appendDescription(hostedBy);
        embedBuilder.appendDescription(giveawayIdDescription);

        if (GiveawayRegistry.getInstance().getUrlImage(guildIdLong) != null) {
            embedBuilder.setImage(GiveawayRegistry.getInstance().getUrlImage(guildIdLong));
        }

        return embedBuilder;
    }
}