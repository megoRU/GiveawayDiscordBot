package main.giveaway;

import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public interface GiftHelper {

    JSONParsers jsonParsers = new JSONParsers();

    default void editMessage(EmbedBuilder embedBuilder, Long guildId, Long textChannel) {
        try {
            if (BotStartConfig.getJda()
                    .getGuildById(guildId)
                    .getSelfMember()
                    .hasPermission(BotStartConfig.getJda().getTextChannelById(textChannel), Permission.VIEW_CHANNEL)) {

                BotStartConfig.getJda()
                        .getGuildById(guildId)
                        .getTextChannelById(textChannel)
                        .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId().get(guildId), embedBuilder.build())
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

    default void updateGiveawayMessage(String endingWord, Long guildId, Long channelId, Integer count) {
        try {
            EmbedBuilder edit = new EmbedBuilder();
            edit.setColor(0x00FF00);
            edit.setTitle(GiveawayRegistry.getInstance().getTitle().get(guildId));

            edit.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", String.valueOf(guildId))
                    .replaceAll("\\{0}", GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null ? "TBA"
                            : GiveawayRegistry.getInstance().getCountWinners().get(guildId))
                    .replaceAll("\\{1}", setEndingWord(endingWord, guildId)) + count + "`");

            //Если есть время окончания включить в EmbedBuilder
            if (!GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId).equals("null")) {
                edit.setTimestamp(OffsetDateTime.parse(String.valueOf(GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId))));
                edit.setFooter(jsonParsers.getLocale("gift_Ends_At", String.valueOf(guildId)));
            }

            if (GiveawayRegistry.getInstance().getIsForSpecificRole().get(guildId) != null
                    && GiveawayRegistry.getInstance().getIsForSpecificRole().get(guildId)) {
                edit.addField(jsonParsers.getLocale("gift_notification", String.valueOf(guildId)),
                        jsonParsers.getLocale("gift_special_role", String.valueOf(guildId))
                                + "<@&" + GiveawayRegistry.getInstance().getRoleId().get(guildId) + ">", false);
            }

            //Отправляет сообщение и если нельзя редактировать то отправляет ошибку
            BotStartConfig.getJda().getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId().get(guildId),
                            edit.build()).queue(null, (exception) ->
                            BotStartConfig.getJda().getTextChannelById(channelId).sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guildId))
                                    .queue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    default void updateGiveawayMessageWithError(String endingWord, Long guildId, Long channelId, Integer count, Integer countWinner) {
        try {
            EmbedBuilder edit = new EmbedBuilder();
            edit.setColor(0x00FF00);
            edit.setTitle(GiveawayRegistry.getInstance().getTitle().get(guildId));

            edit.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", String.valueOf(guildId))
                    .replaceAll("\\{0}", GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null ? "TBA"
                            : GiveawayRegistry.getInstance().getCountWinners().get(guildId))
                    .replaceAll("\\{1}", setEndingWord(endingWord, guildId)) + count + "`");

            edit.addField(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildId)),
                    jsonParsers
                            .getLocale("gift_Invalid_Number_Description", String.valueOf(guildId))
                            .replaceAll("\\{0}", String.valueOf(countWinner))
                            .replaceAll("\\{1}", String.valueOf(count)), false);

            //Если есть время окончания включить в EmbedBuilder
            if (!GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId).equals("null")) {
                edit.setTimestamp(OffsetDateTime.parse(String.valueOf(GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildId))));
                edit.setFooter(jsonParsers.getLocale("gift_Ends_At", String.valueOf(guildId)));
            }

            if (GiveawayRegistry.getInstance().getIsForSpecificRole().get(guildId) != null
                    && GiveawayRegistry.getInstance().getIsForSpecificRole().get(guildId)) {
                edit.addField(jsonParsers.getLocale("gift_notification", String.valueOf(guildId)),
                        jsonParsers.getLocale("gift_special_role", String.valueOf(guildId))
                                + "<@&" + GiveawayRegistry.getInstance().getRoleId().get(guildId) + ">", false);
            }

            //Отправляет сообщение и если нельзя редактировать то отправляет ошибку
            BotStartConfig.getJda().getGuildById(guildId)
                    .getTextChannelById(channelId)
                    .editMessageEmbedsById(GiveawayRegistry.getInstance().getMessageId().get(guildId),
                            edit.build()).queue(null, (exception) ->
                            BotStartConfig.getJda().getTextChannelById(channelId).sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guildId))
                                    .queue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    default String setEndingWord(Object num, long guildId) {
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


}
