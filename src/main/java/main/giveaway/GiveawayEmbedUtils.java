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

    public static EmbedBuilder embedBuilder(final EmbedBuilder embedBuilder, final long guildIdLong, String time) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        String title = instance.getTitle(guildIdLong);
        long createdUserId = instance.getIdUserWhoCreateGiveaway(guildIdLong);
        String giftReaction = jsonParsers.getLocale("gift_reaction", String.valueOf(guildIdLong));
        int countWinners = instance.getCountWinners(guildIdLong);
        String imageUrl = instance.getUrlImage(guildIdLong);
        Long role = instance.getRoleId(guildIdLong);
        boolean isForSpecificRole = instance.getIsForSpecificRole(guildIdLong);

        String footer;
        if (countWinners == 1) {
            footer = String.format("1 %s", GiftHelper.setEndingWord(1, guildIdLong));
        } else {
            footer = String.format("%s %s", countWinners, GiftHelper.setEndingWord(countWinners, guildIdLong));
        }

        String hostedBy = String.format("\nHosted by: <@%s>", createdUserId);
        embedBuilder.setTitle(title);
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setDescription(giftReaction);
        if (isForSpecificRole) {
            String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", String.valueOf(guildIdLong)), role);
            embedBuilder.appendDescription(giftOnlyFor);
        }
        instance.getGift(guildIdLong).setTime(embedBuilder, time);
        embedBuilder.appendDescription(hostedBy);
        embedBuilder.setImage(imageUrl);
        embedBuilder.setFooter(footer);
        return embedBuilder;
    }

    public static EmbedBuilder embedBuilder(final String winners, final int countWinner, final long guildIdLong) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        long idUserWhoCreateGiveaway = instance.getIdUserWhoCreateGiveaway(guildIdLong);
        EmbedBuilder embedBuilder = new EmbedBuilder();

        LOGGER.info("\nEmbedBuilder: " +
                "\nwinners: " + winners +
                "\ncountWinner: " + countWinner
                + "\nguildIdLong: " + guildIdLong);

        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setTitle(instance.getTitle(guildIdLong));

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

        if (instance.getIsForSpecificRole(guildIdLong)) {
            Long roleId = instance.getRoleId(guildIdLong);
            String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", String.valueOf(guildIdLong)), roleId);

            embedBuilder.appendDescription(giftOnlyFor);
        }
        long giveawayIdLong = instance.getMessageId(guildIdLong);

        String hostedBy = String.format("\nHosted by: <@%s>", idUserWhoCreateGiveaway);
        String giveawayIdDescription = String.format("\nGiveaway ID: `%s`", giveawayIdLong);

        embedBuilder.appendDescription(hostedBy);
        embedBuilder.appendDescription(giveawayIdDescription);

        if (instance.getUrlImage(guildIdLong) != null) {
            embedBuilder.setImage(instance.getUrlImage(guildIdLong));
        }

        return embedBuilder;
    }
}