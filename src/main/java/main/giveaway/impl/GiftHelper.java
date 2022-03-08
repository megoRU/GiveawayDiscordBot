package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public interface GiftHelper {

    JSONParsers jsonParsers = new JSONParsers();

    static void editMessage(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        try {
            if (BotStartConfig.getJda().getGuildById(guildId) == null) return;

            if (BotStartConfig.getJda()
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.getJda().getTextChannelById(textChannel), Permission.VIEW_CHANNEL)) {

                BotStartConfig.getJda()
                        .getGuildById(guildId)
                        .getTextChannelById(textChannel)
                        .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId(guildId), embedBuilder.build())
                        .setActionRows().setActionRows(new ArrayList<>())
                        .queue();
            } else {
                if (BotStartConfig.getJda()
                        .getGuildById(guildId)
                        .getSelfMember()
                        .hasPermission(BotStartConfig.getJda().getTextChannelById(textChannel), Permission.MESSAGE_SEND)) {
                    BotStartConfig.getJda()
                            .getGuildById(guildId)
                            .getTextChannelById(textChannel)
                            .sendMessage("Cannot perform action due to a lack of Permission. Missing permission: `VIEW_CHANNEL`")
                            .queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO доделать для Timestamp
    static void updateGiveawayMessage(String endingWord, final long guildId, final long channelId, Integer count) {
        try {
            if (BotStartConfig.getJda().getGuildById(guildId) == null) return;

            EmbedBuilder edit = new EmbedBuilder();
            edit.setColor(0x00FF00);
            edit.setTitle(GiveawayRegistry.getInstance().getTitle(guildId));

            edit.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", String.valueOf(guildId))
                    .replaceAll("\\{0}", GiveawayRegistry.getInstance().getCountWinners(guildId) == null ? "TBA"
                            : GiveawayRegistry.getInstance().getCountWinners(guildId))
                    .replaceAll("\\{1}", setEndingWord(endingWord, guildId)) + count + "`");

            //Если есть время окончания включить в EmbedBuilder
            if (GiveawayRegistry.getInstance().getEndGiveawayDate(guildId) != null) {
                Instant specificTime = Instant.ofEpochMilli(GiveawayRegistry.getInstance().getEndGiveawayDate(guildId).getTime());
                edit.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
                edit.setFooter(jsonParsers.getLocale("gift_Ends_At", String.valueOf(guildId)));
            }

            if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildId) != null
                    && GiveawayRegistry.getInstance().getIsForSpecificRole(guildId)) {
                edit.addField(jsonParsers.getLocale("gift_notification", String.valueOf(guildId)),
                        jsonParsers.getLocale("gift_special_role", String.valueOf(guildId))
                                + "<@&" + GiveawayRegistry.getInstance().getRoleId(guildId) + ">", false);
            }

            //Отправляет сообщение и если нельзя редактировать то отправляет ошибку
            BotStartConfig.getJda().getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId(guildId),
                            edit.build()).queue(null, (exception) ->
                            BotStartConfig.getJda().getTextChannelById(channelId)
                                    .sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guildId))
                                    .queue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO доделать для Timestamp
    static void updateGiveawayMessageWithError(String endingWord, final long guildId, final long channelId, Integer count, Integer countWinner) {
        try {
            if (BotStartConfig.getJda().getGuildById(guildId) == null) return;

            EmbedBuilder edit = new EmbedBuilder();
            edit.setColor(0x00FF00);
            edit.setTitle(GiveawayRegistry.getInstance().getTitle(guildId));

            edit.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", String.valueOf(guildId))
                    .replaceAll("\\{0}", GiveawayRegistry.getInstance().getCountWinners(guildId) == null ? "TBA"
                            : GiveawayRegistry.getInstance().getCountWinners(guildId))
                    .replaceAll("\\{1}", setEndingWord(endingWord, guildId)) + count + "`");

            edit.addField(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildId)),
                    jsonParsers
                            .getLocale("gift_Invalid_Number_Description", String.valueOf(guildId))
                            .replaceAll("\\{0}", String.valueOf(countWinner))
                            .replaceAll("\\{1}", String.valueOf(count)), false);

            //Если есть время окончания включить в EmbedBuilder
            if (GiveawayRegistry.getInstance().getEndGiveawayDate(guildId) != null) {
                Instant specificTime = Instant.ofEpochMilli(GiveawayRegistry.getInstance().getEndGiveawayDate(guildId).getTime());
                edit.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
                edit.setFooter(jsonParsers.getLocale("gift_Ends_At", String.valueOf(guildId)));
            }

            if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildId) != null
                    && GiveawayRegistry.getInstance().getIsForSpecificRole(guildId)) {
                edit.addField(jsonParsers.getLocale("gift_notification", String.valueOf(guildId)),
                        jsonParsers.getLocale("gift_special_role", String.valueOf(guildId))
                                + "<@&" + GiveawayRegistry.getInstance().getRoleId(guildId) + ">", false);
            }

            //Отправляет сообщение и если нельзя редактировать то отправляет ошибку
            BotStartConfig.getJda().getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId(guildId),
                            edit.build()).queue(null, (exception) ->
                            BotStartConfig.getJda().getTextChannelById(channelId)
                                    .sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guildId))
                                    .queue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String setEndingWord(Object num, final long guildId) {
        String language = "eng";
        if (BotStartConfig.getMapLanguages().get(String.valueOf(guildId)) != null) {
            language = BotStartConfig.getMapLanguages().get(String.valueOf(guildId));
        }
        if (num == null) {
            num = "1";
        }

        if (num.equals("TBA")) {
            return language.equals("eng") ? "Winners" : "Победителей";
        }

        return switch (Integer.parseInt((String) num) % 10) {
            case 1 -> language.equals("eng") ? "Winner" : "Победитель";
            case 2, 3, 4 -> language.equals("eng") ? "Winners" : "Победителя";
            default -> language.equals("eng") ? "Winners" : "Победителей";
        };
    }

    static String getMinutes(String time) {
        String symbol = time.substring(time.length() - 1);
        time = time.substring(0, time.length() - 1);

        if (symbol.equals("m") || symbol.equals("м")) {
            return time;
        }

        if (symbol.equals("h") || symbol.equals("ч")) {
            return String.valueOf(Integer.parseInt(time) * 60);
        }

        if (symbol.equals("d") || symbol.equals("д")) {
            return String.valueOf(Integer.parseInt(time) * 1440);
        }
        return "5";
    }

}