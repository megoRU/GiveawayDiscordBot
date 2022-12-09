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
        Giveaway giveaway = instance.getGiveaway(guildIdLong);

        if (giveaway != null) {
            String title = giveaway.getTitle();
            long createdUserId = giveaway.getUserIdLong();
            String giftReaction = jsonParsers.getLocale("gift_reaction", String.valueOf(guildIdLong));
            int countWinners = giveaway.getCountWinners();
            String imageUrl = giveaway.getUrlImage();
            Long role = giveaway.getRoleId();
            boolean isForSpecificRole = giveaway.isForSpecificRole();

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
            giveaway.setTime(embedBuilder, time);
            embedBuilder.appendDescription(hostedBy);
            embedBuilder.setImage(imageUrl);
            embedBuilder.setFooter(footer);
        }

        return embedBuilder;
    }

    public static EmbedBuilder embedBuilder(final String winners, final int countWinner, final long guildIdLong) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(guildIdLong);

        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (giveaway != null) {
            long idUserWhoCreateGiveaway = giveaway.getUserIdLong();
            LOGGER.info("\nEmbedBuilder: " +
                    "\nwinners: " + winners +
                    "\ncountWinner: " + countWinner
                    + "\nguildIdLong: " + guildIdLong);

            embedBuilder.setColor(Color.GREEN);
            embedBuilder.setTitle(giveaway.getTitle());

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

            if (giveaway.isForSpecificRole()) {
                Long roleId = giveaway.getRoleId();
                String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", String.valueOf(guildIdLong)), roleId);

                embedBuilder.appendDescription(giftOnlyFor);
            }
            long giveawayIdLong = giveaway.getMessageId();

            String hostedBy = String.format("\nHosted by: <@%s>", idUserWhoCreateGiveaway);
            String giveawayIdDescription = String.format("\nGiveaway ID: `%s`", giveawayIdLong);

            embedBuilder.appendDescription(hostedBy);
            embedBuilder.appendDescription(giveawayIdDescription);

            if (giveaway.getUrlImage() != null) {
                embedBuilder.setImage(giveaway.getUrlImage());
            }
        }
        return embedBuilder;
    }
}