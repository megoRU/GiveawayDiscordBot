package main.jsonparser;

import main.config.BotStartConfig;

public class JSONParsers {

    public String getLocale(String key, String userIdLong) {
        try {
            return ParserClass.getInstance().getTranslation(key,
                    BotStartConfig.getMapLanguages().get(userIdLong) != null
                    ? BotStartConfig.getMapLanguages().get(userIdLong) : "eng");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NO_FOUND_LOCALIZATION";
    }
}