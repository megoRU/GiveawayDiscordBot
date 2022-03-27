package main.jsonparser;

import main.config.BotStartConfig;

public class JSONParsers {

    public String getLocale(String key, String guildIdLong) {
        try {
            return ParserClass.getInstance().getTranslation(key,
                    BotStartConfig.getMapLanguages().get(guildIdLong) != null
                    ? BotStartConfig.getMapLanguages().get(guildIdLong) : "eng");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}