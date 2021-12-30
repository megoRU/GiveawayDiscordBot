package main.config;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("TOKEN");
    private static final String TOKEN = DEV_BOT_TOKEN;

    private static final String TOP_GG_API_TOKEN = System.getenv("TOP_GG_API_TOKEN");
    private static final String BOT_ID = "808277484524011531"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
    private static final String STATCORD = System.getenv("STATCORD_TOKEN");
    private static final String URL = "https://discord.com/oauth2/authorize?client_id=808277484524011531&permissions=2147511296&scope=applications.commands%20bot";

    public static String getTOKEN() {
        return TOKEN;
    }

    public static String getTopGgApiToken() {
        return TOP_GG_API_TOKEN;
    }

    public static String getBotId() {
        return BOT_ID;
    }

    public static String getStatcord() {
        return STATCORD;
    }
}