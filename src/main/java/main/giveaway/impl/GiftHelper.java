package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.ChecksClass;
import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.EmbedBuilder;

public class GiftHelper {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    public GiftHelper(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void editMessage(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        try {
            ChecksClass checksClass = new ChecksClass(activeGiveawayRepository);

            if (checksClass.isGuildDeleted(guildId)) return;

            BotStartConfig.jda
                    .getGuildById(guildId)
                    .getTextChannelById(textChannel)
                    .retrieveMessageById(GiveawayRegistry.getInstance().getMessageId(guildId))
                    .complete()
                    .editMessageEmbeds(embedBuilder.build())
                    .submit();

        } catch (Exception e) {
            if (e.getMessage().contains("10008: Unknown Message")
                    || e.getMessage().contains("Missing permission: VIEW_CHANNEL")
                    || e.getMessage().contains("net.dv8tion.jda.api.entities.TextChannel.retrieveMessageById(String)")
                    || e.getMessage().contains("net.dv8tion.jda.api.entities.Guild.getTextChannelById(long)")

            ) {
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

    public static String getMinutes(String time) {
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

        throw new IllegalArgumentException("Argument don`t have symbol: " + time);
    }

}