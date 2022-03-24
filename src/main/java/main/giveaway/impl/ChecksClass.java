package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.GiveawayRegistry;
import main.model.repository.ActiveGiveawayRepository;

public class ChecksClass {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    public ChecksClass(ActiveGiveawayRepository activeGiveawayRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public boolean isGuildDeleted(final long guildId) {
        if (BotStartConfig.jda.getGuildById(guildId) != null) {
            return false;
        } else {
            System.out.println("Бота нет в Guild -> Удаляем Giveaway!");
            activeGiveawayRepository.deleteActiveGiveaways(guildId);
            GiveawayRegistry.getInstance().removeGift(guildId);
            return true;
        }
    }
}