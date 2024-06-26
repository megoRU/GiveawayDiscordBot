package main.config;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("TOKEN");
    private static final String TOKEN = PRODUCTION_BOT_TOKEN;
    private static final String BOT_ID = "808277484524011531"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
    private static final String BOTICORD = System.getenv("BOTICORD");

    private static volatile boolean IS_DEV = true;

    static {
        if (TOKEN.equals(PRODUCTION_BOT_TOKEN)) {
            IS_DEV = false;
        }
    }

    public static String getTOKEN() {
        return TOKEN;
    }

    public static String getBotId() {
        return BOT_ID;
    }

    public static boolean isIsDev() {
        return IS_DEV;
    }

    public static String getBoticord() {
        return BOTICORD;
    }
}