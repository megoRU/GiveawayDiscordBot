package main.giveaway;

import main.jsonparser.JSONParsers;
import main.service.SlashService;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.sql.Timestamp;
import java.time.Instant;

public class GiveawayEmbedUtils {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public static EmbedBuilder giveawayPattern(GiveawayData giveawayData, Giveaway giveaway) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        long guildId = giveaway.getGuildId();

        Color userColor = GiveawayUtils.getUserColor(guildId);

        String title = giveawayData.getTitle();
        long createdUserId = giveaway.getUserIdLong();
        String giftReaction = jsonParsers.getLocale("gift_reaction", guildId);
        int countWinners = giveawayData.getCountWinners();
        String imageUrl = giveawayData.getUrlImage();
        Long role = giveawayData.getRoleId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        Timestamp endGiveaway = giveawayData.getEndGiveawayDate();

        //Title
        embedBuilder.setTitle(title);
        //Color
        embedBuilder.setColor(userColor);

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
            if (role != guildId) {
                String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), role);
                embedBuilder.appendDescription(giftOnlyFor);
            }
        }

        //EndGiveaway
        if (endGiveaway != null) {
            long endTime = endGiveaway.getTime() / 1000;
            String endTimeFormat =
                    String.format(jsonParsers.getLocale("gift_ends_giveaway", guildId), endTime, endTime);
            embedBuilder.appendDescription(endTimeFormat);
        }

        String giftHosted = String.format(jsonParsers.getLocale("gift_hosted", guildId), createdUserId);

        //Hosted By
        embedBuilder.appendDescription(giftHosted);
        //Image
        embedBuilder.setImage(imageUrl);
        //Footer
        embedBuilder.setFooter(footer);

        return embedBuilder;
    }

    @Nullable
    public static EmbedBuilder giveawayEnd(final String winners, int countWinners, final long guildId, long messageId) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(messageId);
        Color userColor = GiveawayUtils.getUserColor(guildId);

        if (giveaway != null) {
            GiveawayData giveawayData = giveaway.getGiveawayData();
            String title = giveawayData.getTitle();
            long createdUserId = giveaway.getUserIdLong();

            embedBuilder.setColor(userColor);
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

            if (giveawayData.isForSpecificRole()) {
                Long roleId = giveawayData.getRoleId();

                if (roleId != guildId) {
                    String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);
                    embedBuilder.appendDescription(giftOnlyFor);
                }
            }

            String giftHosted = String.format(jsonParsers.getLocale("gift_hosted", guildId), createdUserId);
            String rerollParsers = String.format(jsonParsers.getLocale("reroll", guildId), createdUserId);
            Long reroll = SlashService.getCommandId("reroll");

            String giveawayIdDescription = String.format("\n\nGiveaway ID: `%s`", messageId);
            String giveawayReroll = String.format("\n%s </reroll:%s>", rerollParsers, reroll);

            //Hosted By
            embedBuilder.appendDescription(giftHosted);
            //Giveaway ID
            embedBuilder.appendDescription(giveawayIdDescription);
            //Reroll
            embedBuilder.appendDescription(giveawayReroll);

            if (giveawayData.getUrlImage() != null) {
                embedBuilder.setImage(giveawayData.getUrlImage());
            }
            return embedBuilder;
        } else {
            return null;
        }
    }
}