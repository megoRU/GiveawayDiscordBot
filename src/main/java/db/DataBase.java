package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

  //CREATE TABLE `guildId` (`id` bigint(30) NOT NULL, `user_long_id` bigint(30) NOT NULL,
  // PRIMARY KEY (`id`),
  // UNIQUE KEY `id` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  private static Connection connection;

  //Создаем один коннект на программу
  private static Connection getConnection() throws SQLException {
    if (connection == null) {
      connection = DriverManager.getConnection(Config.getGiveawayConnection(), Config.getGiveawayUser(), Config.getGiveawayPass());
    }
    return connection;
  }

  public DataBase() throws SQLException {}

  //Создаем таблицу когда кто-то создал Giveaway
  public void createTableWhenGiveawayStart(String guildLongId) {
    try {
      String query = "CREATE TABLE `" + guildLongId + "` (`user_long_id` bigint(30) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
      preparedStmt.executeUpdate();
      preparedStmt.close();
    } catch (Exception e) {
      dropTableWhenGiveawayStop(guildLongId);
      createTableWhenGiveawayStart(guildLongId);
      e.printStackTrace();
      System.err.println("Recursive call");
    }
  }

  //Удаляем таблицу когда кто-то остановил Giveaway
  public void dropTableWhenGiveawayStop(String guildLongId) {
    try {
      String query = "DROP TABLE `" + guildLongId + "`;";
      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
      preparedStmt.executeUpdate();
      preparedStmt.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void insertUserToDB(String guildIdLong, long userIdLong) {
    try {
      String query = "INSERT IGNORE INTO `" + guildIdLong + "` (user_long_id) values (?)";
      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
      preparedStmt.setLong(1, userIdLong);
      preparedStmt.executeUpdate();
      preparedStmt.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //Добавление префикса
  public void addPrefixToDB(String serverId, String prefix) {
    try {
      String sql = "INSERT INTO prefixs (serverId, prefix) VALUES (?, ?)";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.setString(1, serverId);
      preparedStatement.setString(2, prefix);
      preparedStatement.execute();
      preparedStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Удаление префикса
  public void removePrefixFromDB(String serverId) {
    try {
      String sql = "DELETE FROM prefixs WHERE serverId='" + serverId + "'";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.execute(sql);
      preparedStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Удаление строки с id long message
  public void removeMessageFromDB(long guildId) {
    try {
      String sql = "DELETE FROM ActiveGiveaways WHERE guild_long_id='" + guildId + "'";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.execute(sql);
      preparedStatement.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Добавляем в Бд данные о id long message
  public void addMessageToDB(long guildId, long messageId, long channelId, String countWinners, String date, String title, String endDate) {
    try {
      String sql = "INSERT INTO ActiveGiveaways ("
          + "guild_long_id, "
          + "message_id_long, "
          + "channel_id_long, "
          + "count_winners, "
          + "date_end_giveaway, "
          + "giveaway_title, "
          + "end_time) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.setLong(1, guildId);
      preparedStatement.setLong(2, messageId);
      preparedStatement.setLong(3, channelId);
      preparedStatement.setString(4, countWinners);
      preparedStatement.setString(5, date);
      preparedStatement.setString(6, title);
      preparedStatement.setString(7, endDate);
      preparedStatement.execute();
      preparedStatement.close();
    } catch (SQLException e) {
      removeMessageFromDB(guildId);
      addMessageToDB(guildId, messageId, channelId, countWinners, date, title, endDate);
      e.printStackTrace();
      System.err.println("Recursive call");
    }
  }

}