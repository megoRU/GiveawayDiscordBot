package config;

public class Config {

  private static final String DEV_BOT_TOKEN = "";
  private static final String PRODUCTION_BOT_TOKEN = "";
  private static final String TOKEN = DEV_BOT_TOKEN;

  //Данный от БД с префиксами
  private static final String PREFIX_CONNECTION = ""; //utf8mb4
  private static final String PREFIX_USER = "";
  private static final String PREFIX_PASS = "";

  //Данный от БД с Giveaways
  private static final String GIVEAWAY_CONNECTION = ""; //utf8mb4
  private static final String GIVEAWAY_USER = "";
  private static final String GIVEAWAY_PASS = "";

  private static final String TOP_GG_API_TOKEN = "";
  private static final String BOT_ID = "808277484524011531"; //megoDev: 780145910764142613 //giveaway: 808277484524011531
  private static final String STATCORD = "";
  private static final String URL = "https://discord.com/oauth2/authorize?client_id=780145910764142613&scope=bot&permissions=11328";

  public static String getTOKEN() {
    return TOKEN;
  }

  public static String getPrefixConnection() {
    return PREFIX_CONNECTION;
  }

  public static String getPrefixUser() {
    return PREFIX_USER;
  }

  public static String getPrefixPass() {
    return PREFIX_PASS;
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