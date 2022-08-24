package main.jsonparser;

import main.config.BotStartConfig;

public class JSONParsers {

    public String getLocale(String key, String guildIdLong) {
        try {
            String language = BotStartConfig.getMapLanguages().get(guildIdLong);
            return ParserClass.getInstance().getTranslation(key, language != null ? language : "eng");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}