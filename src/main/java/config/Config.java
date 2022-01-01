package config;

public class Config {

  private static final String DEV_BOT_TOKEN = System.getenv("DEV_TOKEN");
  private static final String PRODUCTION_BOT_TOKEN = System.getenv("TOKEN");
  private static final String TOKEN = PRODUCTION_BOT_TOKEN;
  private static final String GIVEAWAY_NAME = "DiscordBotGiveaway";

  //Данный от БД с Giveaways

  private static final String GIVEAWAY_CONNECTION = "jdbc:mysql://45.140.167.181:3306/" + GIVEAWAY_NAME + "?autoReconnect=true&useSSL=false&serverTimezone=UTC&characterEncoding=utf8"; //utf8mb4

  private static final String GIVEAWAY_USER = GIVEAWAY_NAME;
  private static final String GIVEAWAY_PASS = System.getenv("PASS");

  private static final String TOP_GG_API_TOKEN = System.getenv("TOP_GG_API_TOKEN");
  private static final String BOT_ID = "808277484524011531"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
  private static final String STATCORD = System.getenv("STATCORD");
  private static final String URL = "https://discord.com/oauth2/authorize?client_id=808277484524011531&permissions=2147511296&scope=applications.commands%20bot";

  public static String getTOKEN() {
    return TOKEN;
  }

  public static String getGiveawayConnection() {
    return GIVEAWAY_CONNECTION;
  }

  public static String getGiveawayUser() {
    return GIVEAWAY_USER;
  }

  public static String getGiveawayPass() {
    return GIVEAWAY_PASS;
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