package main.giveaway.impl;

import main.config.BotStart;

public interface WordBuilder {

    static String setEndingWord(int num, final long guildId) {
        String language = "eng";
        String languageFrom = BotStart.getMapLanguages().get(String.valueOf(guildId));
        if (languageFrom != null) {
            language = languageFrom;
        }

        return switch (num % 10) {
            case 1 -> language.equals("eng") ? "Winner" : "Победитель";
            case 2, 3, 4 -> language.equals("eng") ? "Winners" : "Победителя";
            default -> language.equals("eng") ? "Winners" : "Победителей";
        };
    }
}
