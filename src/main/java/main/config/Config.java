package main.config;

public class Config {

    private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
    private static final String PRODUCTION_BOT_TOKEN = System.getenv("TOKEN");
    private static final String TOKEN = PRODUCTION_BOT_TOKEN;
    public static final Boolean IS_PROXY = Boolean.parseBoolean(System.getenv("IS_PROXY"));
    public static final String PROXY_IP = String.valueOf(System.getenv("PROXY_IP"));

    private static volatile boolean IS_DEV = true;

    static {
        if (TOKEN.equals(PRODUCTION_BOT_TOKEN)) {
            IS_DEV = false;
        }
    }

    public static String getTOKEN() {
        return TOKEN;
    }

    public static boolean isIsDev() {
        return IS_DEV;
    }
}