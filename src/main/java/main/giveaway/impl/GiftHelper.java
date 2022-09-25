package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class GiftHelper {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    public GiftHelper(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }


    public void editMessage(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        try {
            //TODO: Удалить
//            ChecksClass checksClass = new ChecksClass(activeGiveawayRepository);
//
//            if (checksClass.isGuildDeleted(guildId)) return;

            Guild guildById = BotStartConfig.getJda().getGuildById(guildId);
            if (guildById != null) {
                TextChannel textChannelById = guildById.getTextChannelById(textChannel);

                if (textChannelById != null) {
                    textChannelById
                            .retrieveMessageById(GiveawayRegistry.getInstance().getMessageId(guildId))
                            .complete()
                            .editMessageEmbeds(embedBuilder.build())
                            .submit();
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("10008: Unknown Message")
                    || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                System.out.println(e.getMessage() + " удаляем!");
                activeGiveawayRepository.deleteActiveGiveaways(guildId);
                GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
            } else {
                e.printStackTrace();
            }
        }
    }

    public static String setEndingWord(int num, final long guildId) {
        String language = "eng";
        if (BotStartConfig.getMapLanguages().get(String.valueOf(guildId)) != null) {
            language = BotStartConfig.getMapLanguages().get(String.valueOf(guildId));
        }

        return switch (num % 10) {
            case 1 -> language.equals("eng") ? "Winner" : "Победитель";
            case 2, 3, 4 -> language.equals("eng") ? "Winners" : "Победителя";
            default -> language.equals("eng") ? "Winners" : "Победителей";
        };
    }

    public static long getSeconds(String time) {
        String[] splitTime = time.split("\\s+");
        long seconds = 0;

        for (String s : splitTime) {
            long localTime = Long.parseLong(s.substring(0, s.length() - 1));
            String symbol = s.substring(s.length() - 1);

            switch (symbol) {
                case "m", "м" -> seconds += localTime * 60;
                case "h", "ч" -> seconds += localTime * 3600;
                case "d", "д" -> seconds += localTime * 86400;
                case "s", "с" -> seconds += localTime;
            }
        }

        return seconds;
//        throw new IllegalArgumentException("Argument don`t have symbol: " + time);
    }
}