package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.concurrent.CompletableFuture;

public class GiftHelper {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    public GiftHelper(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void editMessage(EmbedBuilder embedBuilder, final long guildId, final long textChannel) {
        try {
            ChecksClass checksClass = new ChecksClass(activeGiveawayRepository);

            if (checksClass.isGuildDeleted(guildId)) return;

            CompletableFuture<Message> action = BotStartConfig.jda
                    .getGuildById(guildId)
                    .getTextChannelById(textChannel)
                    .retrieveMessageById(GiveawayRegistry.getInstance().getMessageId(guildId))
                    .submit();

            action.thenCompose(message -> message.editMessageEmbeds(embedBuilder.build())
                    .submit()
                    .whenComplete((message1, throwable) -> {
                        if (throwable != null) {
                            System.out.println(throwable.getMessage());
                            if (throwable.getMessage().contains("10008: Unknown Message")) {
                                System.out.println("10008: Unknown Message: Удаляем Giveaway!");
                                activeGiveawayRepository.deleteActiveGiveaways(guildId);
                            }
                        }
                    }));
            //TODO .orTimeout(10000L, TimeUnit.MILLISECONDS)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Message> getMessageDescription(final long guildId, final long textChannelId) {
      return BotStartConfig.jda
                .getGuildById(guildId)
                .getTextChannelById(textChannelId)
                .retrieveMessageById(GiveawayRegistry.getInstance().getMessageId(guildId))
                .submit();
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
        return "5";
    }

}