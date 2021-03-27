import config.Config;
import giveaway.Gift;
import giveaway.GiveawayRegistry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import startbot.BotStart;
import threads.StatcordThread;
import threads.TopGGApiThread;

public class Main {

  public static void main(String[] args) throws Exception {
    //Загружаем данные в коллекции. Может лучше создать отдельный метод в классе BotStart
    //TODO: Загружаем данные в коллекции. Может лучше создать отдельный метод в классе BotStart
    // Реализовать авто-завершение Giveaways по таймеру, также под это реализовать логику в Gift и MessageGift



    Map<Integer, String> guildIdHashList = new HashMap<>();


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

    try {
      Connection conn = DriverManager.getConnection(Config.getGiveawayConnection(), Config.getGiveawayUser(), Config.getGiveawayPass());
      Statement statement2 = conn.createStatement();
      for (int i = 1; i <= guildIdHashList.size(); i++) {
        String sql2 = "select * from `" + guildIdHashList.get(i) + "`;";
        ResultSet rs2 = statement2.executeQuery(sql2);
        while (rs2.next()) {

          long userIdLong = rs2.getLong("user_long_id");

          System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

          //Добавляем пользователей в hashmap
          GiveawayRegistry.getInstance()
              .getActiveGiveaways()
              .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash()
              .put(String.valueOf(userIdLong), String.valueOf(userIdLong));

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
      conn.close();
      statement2.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    GiveawayRegistry.getInstance().setGift(31231, new Gift());

    BotStart botStart = new BotStart();
    botStart.startBot();
    GiveawayRegistry.getInstance();

//    StatcordThread statcordThread = new StatcordThread();
//    statcordThread.start();
//
//    TopGGApiThread topGGApiThread = new TopGGApiThread();
//    topGGApiThread.start();
  }

}
