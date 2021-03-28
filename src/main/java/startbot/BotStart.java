package startbot;

import config.Config;
import events.MessageWhenBotJoinToGuild;
import giveaway.Gift;
import giveaway.GiveawayRegistry;
import giveaway.MessageGift;
import giveaway.Reactions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import messagesevents.MessageInfoHelp;
import messagesevents.PrefixChange;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class BotStart {
  //TODO: Реализовать авто-завершение Giveaways по таймеру, также под это реализовать логику в Gift и MessageGift
  private static JDA jda;
  private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
  private static final Map<String, String> mapPrefix = new HashMap<>();
  private static final Map<Integer, String> guildIdHashList = new HashMap<>();

  public void startBot() throws Exception {
    //Получаем id guild и id message
    getMessageIdFromDB();

    //Получаем всех участников по гильдиям
    getUsersWhoTakePartFromDB();

    //Получаем все префиксы из базы данных
    getPrefixFromDB();

    jdaBuilder.setAutoReconnect(true);
    jdaBuilder.setStatus(OnlineStatus.ONLINE);
    jdaBuilder.setActivity(Activity.playing("—> !help"));
    jdaBuilder.setBulkDeleteSplittingEnabled(false);
    jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
    jdaBuilder.addEventListeners(new MessageGift());
    jdaBuilder.addEventListeners(new PrefixChange());
    jdaBuilder.addEventListeners(new MessageInfoHelp());
    jdaBuilder.addEventListeners(new Reactions());

    jda = jdaBuilder.build();
    jda.awaitReady();

    try {
      Connection connection = DriverManager.getConnection(Config.getPrefixConnection(), Config.getPrefixUser(), Config.getPrefixPass());
      Statement statement = connection.createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }

      connection.close();
      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getMessageIdFromDB() {
    try {
      Connection connection = DriverManager.getConnection(Config.getPrefixConnection(), Config.getPrefixUser(), Config.getPrefixPass());
      Statement statement = connection.createStatement();
      String sql = "select * from ActiveGiveaways";
      ResultSet rs = statement.executeQuery(sql);
      while (rs.next()) {

        long guild_long_id = rs.getLong("guild_long_id");
        long message_id_long = rs.getLong("message_id_long");
//        System.out.println("guild id:" + guild_long_id + " id: сообщения: " + message_id_long);
        //get end date

        guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(guild_long_id));

        GiveawayRegistry.getInstance().setGift(guild_long_id, new Gift(guild_long_id));
        GiveawayRegistry.getInstance().getMessageId().put(guild_long_id, String.valueOf(message_id_long));
        GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().put(guild_long_id, String.valueOf(message_id_long));

      }
      rs.close();
      statement.close();
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void getUsersWhoTakePartFromDB() {
    try {
      Connection conn = DriverManager.getConnection(Config.getGiveawayConnection(), Config.getGiveawayUser(), Config.getGiveawayPass());
      Statement statement = conn.createStatement();

      for (int i = 1; i <= guildIdHashList.size(); i++) {
        String sql = "select * from `" + guildIdHashList.get(i) + "`;";
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {

          long userIdLong = rs.getLong("user_long_id");

          System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

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

      }
      statement.close();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    guildIdHashList.clear();
  }

  private void getPrefixFromDB() {
    try {
      Connection connection = DriverManager.getConnection(Config.getPrefixConnection(), Config.getPrefixUser(), Config.getPrefixPass());
      Statement statement = connection.createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }

      connection.close();
      rs.close();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, String> getMapPrefix() {
    return mapPrefix;
  }

  public static JDA getJda() {
    return jda;
  }
}