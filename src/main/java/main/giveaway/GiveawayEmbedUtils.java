package main.giveaway;

import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.service.SlashService;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;

public class GiveawayEmbedUtils {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public static EmbedBuilder giveawayPattern(Giveaway giveaway) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        long guildId = giveaway.getGuildId();
        long userIdLong = giveaway.getUserIdLong();
        Color userColor = GiveawayUtils.getUserColor(guildId);

        String title = giveaway.getTitle();
        long createdUserId = giveaway.getUserIdLong();
        String giftReaction = jsonParsers.getLocale("gift_reaction", guildId);
        int countWinners = giveaway.getCountWinners();
        String imageUrl = giveaway.getUrlImage();
        Long role = giveaway.getRoleId();
        boolean isForSpecificRole = giveaway.isForSpecificRole();
        Instant endGiveaway = giveaway.getEndGiveawayDate();

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
        long endTime = GiveawayUtils.getEpochSecond(endGiveaway, userIdLong);

        String endTimeFormat = String.format(jsonParsers.getLocale("gift_ends_giveaway", guildId), endTime, endTime);
        embedBuilder.appendDescription(endTimeFormat);

        String giftHosted = String.format(jsonParsers.getLocale("gift_hosted", guildId), createdUserId);

        //Hosted By
        embedBuilder.appendDescription(giftHosted);
        //Image
        embedBuilder.setImage(imageUrl);
        //Footer
        embedBuilder.setFooter(footer);

        return embedBuilder;
    }

    public static EmbedBuilder giveawayPattern(ActiveGiveaways activeGiveaways) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        long guildId = activeGiveaways.getGuildId();
        long userIdLong = activeGiveaways.getCreatedUserId();
        Color userColor = GiveawayUtils.getUserColor(guildId);

        String title = activeGiveaways.getTitle() == null ? "Giveaway" : activeGiveaways.getTitle();
        long createdUserId = activeGiveaways.getCreatedUserId();
        String giftReaction = jsonParsers.getLocale("gift_reaction", guildId);
        int countWinners = activeGiveaways.getCountWinners();
        String imageUrl = activeGiveaways.getUrlImage();
        Long role = activeGiveaways.getRoleId();
        boolean isForSpecificRole = Boolean.TRUE.equals(activeGiveaways.getIsForSpecificRole());
        Instant endGiveaway = activeGiveaways.getEndGiveawayDate();

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
        if (isForSpecificRole && role != null) {
            if (role != guildId) {
                String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), role);
                embedBuilder.appendDescription(giftOnlyFor);
            }
        }

        //EndGiveaway
        long endTime = GiveawayUtils.getEpochSecond(endGiveaway, userIdLong);

        String endTimeFormat = String.format(jsonParsers.getLocale("gift_ends_giveaway", guildId), endTime, endTime);
        embedBuilder.appendDescription(endTimeFormat);

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
    public static EmbedBuilder giveawayEnd(final String winners, int countWinners, final long guildId, long messageId, Giveaway giveaway) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Color userColor = GiveawayUtils.getUserColor(guildId);

        if (giveaway != null) {
            String title = giveaway.getTitle();
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

            if (giveaway.isForSpecificRole()) {
                Long roleId = giveaway.getRoleId();

                if (roleId != null && roleId != guildId) {
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

            if (giveaway.getUrlImage() != null) {
                embedBuilder.setImage(giveaway.getUrlImage());
            }
            return embedBuilder;
        } else {
            return null;
        }
    }

    @Nullable
    public static EmbedBuilder giveawayEnd(final String winners, int countWinners, final long guildId, long messageId, ActiveGiveaways activeGiveaways) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        Color userColor = GiveawayUtils.getUserColor(guildId);

        if (activeGiveaways != null) {
            String title = activeGiveaways.getTitle() == null ? "Giveaway" : activeGiveaways.getTitle();
            long createdUserId = activeGiveaways.getCreatedUserId();

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

            if (Boolean.TRUE.equals(activeGiveaways.getIsForSpecificRole())) {
                Long roleId = activeGiveaways.getRoleId();

                if (roleId != null && roleId != guildId) {
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

            if (activeGiveaways.getUrlImage() != null) {
                embedBuilder.setImage(activeGiveaways.getUrlImage());
            }
            return embedBuilder;
        } else {
            return null;
        }
    }
}