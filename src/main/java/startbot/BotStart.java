package startbot;

import config.Config;
import db.DataBase;
import events.MessageWhenBotJoinToGuild;
import giveaway.Gift;
import giveaway.GiveawayRegistry;
import giveaway.MessageGift;
import giveaway.ReactionsButton;
import jsonparser.JSONParsers;
import messagesevents.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import threads.Giveaway;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BotStart {

  public static final String activity = "!help | ";
  public static final String version = "v15 ";
  private final JSONParsers jsonParsers = new JSONParsers();
  private static JDA jda;
  private static final Deque<Giveaway> queue = new ArrayDeque<>();
  private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
  private static final Map<String, String> disabledGuildForNews = new HashMap<>();
  private static final Map<String, String> mapPrefix = new HashMap<>();
  private static final ConcurrentMap<String, String> mapLanguages = new ConcurrentHashMap<>();
  private static final Map<Integer, String> guildIdHashList = new HashMap<>();

  public void startBot() throws Exception {
    //Получаем языки
    getLocalizationFromDB();

    //Получаем гильдии которые запретили рассылку
    getDisabledGuildsForNews();

    //Получаем id guild и id message
    getMessageIdFromDB();

    //Получаем всех участников по гильдиям
    getUsersWhoTakePartFromDB();

    //Получаем все префиксы из базы данных
    getPrefixFromDB();

    //TopGGAndStatcordThread.serverCount guilds |
    jdaBuilder.setAutoReconnect(true);
    jdaBuilder.setStatus(OnlineStatus.ONLINE);
    jdaBuilder.setActivity(Activity.playing(activity + " New update. If you created Giveaway before June 11, please re-create it !gift stop"));
    jdaBuilder.setBulkDeleteSplittingEnabled(false);
    jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
    jdaBuilder.addEventListeners(new MessageGift());
    jdaBuilder.addEventListeners(new PrefixChange());
    jdaBuilder.addEventListeners(new MessageInfoHelp());
    jdaBuilder.addEventListeners(new LanguageChange());
    jdaBuilder.addEventListeners(new ReactionsButton());
    jdaBuilder.addEventListeners(new MessageNews());
    jdaBuilder.addEventListeners(new SendingMessagesToGuilds());

    jda = jdaBuilder.build();
    jda.awaitReady();

  }

  private void getMessageIdFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from ActiveGiveaways";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {

        long guild_long_id = rs.getLong("guild_long_id");
        String channel_long_id = rs.getString("channel_id_long");
        String count_winners = rs.getString("count_winners");
        long message_id_long = rs.getLong("message_id_long");
        String giveaway_title = rs.getString("giveaway_title");
        String date_end_giveaway = rs.getString("date_end_giveaway");

        guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(guild_long_id));
        GiveawayRegistry.getInstance().setGift(guild_long_id, new Gift(guild_long_id, Long.parseLong(channel_long_id)));
        GiveawayRegistry.getInstance().getActiveGiveaways().get(guild_long_id).autoInsert();
        GiveawayRegistry.getInstance().getMessageId().put(guild_long_id, String.valueOf(message_id_long));
        GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild_long_id, String.valueOf(message_id_long));
        GiveawayRegistry.getInstance().getTitle().put(guild_long_id, giveaway_title);
        GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild_long_id, date_end_giveaway == null ? "null" : date_end_giveaway);
        GiveawayRegistry.getInstance().getChannelId().put(guild_long_id, channel_long_id);
        GiveawayRegistry.getInstance().getCountWinners().put(guild_long_id, count_winners);

        //Добавляем кнопки для Giveaway в Gift class
        setButtonsInGift(String.valueOf(guild_long_id), guild_long_id);

        if (date_end_giveaway != null) {
          queue.add(new Giveaway(guild_long_id, date_end_giveaway));
        }
      }
      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getUsersWhoTakePartFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      System.out.println("Получаем данные с БД и добавляем их в коллекции и экземпляры классов");

      for (int i = 1; i <= guildIdHashList.size(); i++) {
        String sql = "select * from `" + guildIdHashList.get(i) + "`;";
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {

          long userIdLong = rs.getLong("user_long_id");

          //System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

          //Добавляем пользователей в hashmap
          GiveawayRegistry.getInstance()
              .getActiveGiveaways()
              .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash()
              .put(String.valueOf(userIdLong), String.valueOf(userIdLong));

          //Считаем пользователей в hashmap и устанавливаем верное значение
          GiveawayRegistry.getInstance()
              .getActiveGiveaways()
              .get(Long.parseLong(guildIdHashList.get(i))).getListUsers()
              .add(String.valueOf(userIdLong));

          //Устанавливаем счетчик на верное число
          GiveawayRegistry.getInstance()
              .getActiveGiveaways()
              .get(Long.parseLong(guildIdHashList.get(i))).setCount(GiveawayRegistry.getInstance()
              .getActiveGiveaways()
              .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash().size());
        }
        rs.close();
      }
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    guildIdHashList.clear();
  }

  private void setButtonsInGift(String guildId, long longGuildId) {
    if (getMapLanguages().get(guildId) != null) {
      if (getMapLanguages().get(guildId).equals("rus")) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                .add(ActionRow.of(Button.success(longGuildId + ":" + ReactionsButton.emojiPresent,
                        jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀ ⠀⠀")));
      } else {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                .add(ActionRow.of(Button.success(longGuildId + ":" + ReactionsButton.emojiPresent,
                        jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀⠀⠀⠀⠀⠀⠀⠀")));
      }
    } else {
      GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
              .add(ActionRow.of(Button.success(longGuildId + ":" + ReactionsButton.emojiPresent,
                      jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀⠀⠀⠀⠀⠀⠀⠀")));
    }

    GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
            .add(ActionRow.of(Button.danger(longGuildId + ":" + ReactionsButton.emojiStopOne,
                    jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "1"))));

    GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
            .add(ActionRow.of(Button.danger(longGuildId + ":" + ReactionsButton.emojiStopTwo,
                    jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "2"))));

    GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
            .add(ActionRow.of(Button.danger(longGuildId + ":" + ReactionsButton.emojiStopThree,
                    jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "3"))));
  }

  private void getDisabledGuildsForNews() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "SELECT guild_id, is_can_send_news FROM sendNews WHERE is_can_send_news = false";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        getDisabledGuildForNews().put(rs.getString("guild_id"), rs.getString("guild_id"));
      }

      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getPrefixFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }

      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getLocalizationFromDB() {
    try {
      Statement statement = DataBase.getConnection().createStatement();
      String sql = "select * from language";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapLanguages.put(rs.getString("serverId"), rs.getString("lang"));
      }

      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, String> getMapPrefix() {
    return mapPrefix;
  }

  public static Map<String, String> getMapLanguages() {
    return mapLanguages;
  }

  public static Map<String, String> getDisabledGuildForNews() {
    return disabledGuildForNews;
  }

  public static JDA getJda() {
    return jda;
  }

  public static Deque<Giveaway> getQueue() {
    return queue;
  }
}