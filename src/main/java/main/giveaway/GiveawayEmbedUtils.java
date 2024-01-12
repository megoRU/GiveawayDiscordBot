package main.giveaway;

import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;

public class GiveawayEmbedUtils {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public static EmbedBuilder giveawayLayout(final long guildId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(guildId);

        if (giveaway != null) {
            String title = giveaway.getTitle();
            long createdUserId = giveaway.getUserIdLong();
            String giftReaction = jsonParsers.getLocale("gift_reaction", guildId);
            int countWinners = giveaway.getCountWinners();
            String imageUrl = giveaway.getUrlImage();
            Long role = giveaway.getRoleId();
            boolean isForSpecificRole = giveaway.isForSpecificRole();
            Timestamp endGiveaway = giveaway.getEndGiveawayDate();

            //Title
            embedBuilder.setTitle(title);
            //Color
            embedBuilder.setColor(Color.GREEN);

            String footer;
            if (countWinners == 1) {
                footer = String.format("1 %s", GiveawayUtils.setEndingWord(1, guildId));
            } else {
                footer = String.format("%s %s", countWinners, GiveawayUtils.setEndingWord(countWinners, guildId));
            }

            //Reaction
            embedBuilder.setDescription(giftReaction);

            //Giveaway only for Role
            if (isForSpecificRole) {
                String giftOnlyFor;
                if (role == guildId) {
                    giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), role)
                            .replace("<@&" + guildId + ">", "@everyone");
                } else {
                    giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), role);
                }
                embedBuilder.appendDescription(giftOnlyFor);
            }

            //EndGiveaway
            if (endGiveaway != null) {
                long endTime = endGiveaway.getTime() / 1000;
                String endTimeFormat = String.format(jsonParsers.getLocale("gift_ends_giveaway", guildId), endTime, endTime);
                embedBuilder.appendDescription(endTimeFormat);
            }

            String giftHosted = String.format(jsonParsers.getLocale("gift_hosted", guildId), createdUserId);

            //Hosted By
            embedBuilder.appendDescription(giftHosted);
            //Image
            embedBuilder.setImage(imageUrl);
            //Footer
            embedBuilder.setFooter(footer);
        }
        return embedBuilder;
    }

    public static EmbedBuilder giveawayEnd(final String winners, int countWinners, final long guildId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(guildId);

        if (giveaway != null) {
            String title = giveaway.getTitle();
            long createdUserId = giveaway.getUserIdLong();

            embedBuilder.setColor(Color.GREEN);
            embedBuilder.setTitle(title);

            if (countWinners == 1) {
                String giftWinner = String.format(jsonParsers.getLocale("gift_winner", guildId), winners);
                embedBuilder.appendDescription(giftWinner);
            } else {
                String giftWinners = String.format(jsonParsers.getLocale("gift_winners", guildId), winners);
                embedBuilder.appendDescription(giftWinners);
            }

            String footer = countWinners + " " + GiveawayUtils.setEndingWord(countWinners, guildId);
            embedBuilder.setTimestamp(Instant.now());
            String giftEnds = String.format(jsonParsers.getLocale("gift_ends", guildId), footer);
            embedBuilder.setFooter(giftEnds);

            if (giveaway.isForSpecificRole()) {
                Long roleId = giveaway.getRoleId();
                String giftOnlyFor;

                if (roleId == guildId) {
                    giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId)
                            .replaceAll("<@&" + guildId + ">", "@everyone");
                } else {
                    giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);
                }

                embedBuilder.appendDescription(giftOnlyFor);
            }
            long giveawayIdLong = giveaway.getMessageId();

            String giftHosted = String.format(jsonParsers.getLocale("gift_hosted", guildId), createdUserId);
            String giveawayIdDescription = String.format("\n\nGiveaway ID: `%s`", giveawayIdLong);

            embedBuilder.appendDescription(giftHosted);
            embedBuilder.appendDescription(giveawayIdDescription);

            if (giveaway.getUrlImage() != null) {
                embedBuilder.setImage(giveaway.getUrlImage());
            }
        }
        return embedBuilder;
    }
}