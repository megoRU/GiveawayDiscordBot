package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

  //CREATE TABLE `guildId`
  // (`id` bigint(30) NOT NULL,
  // `user_long_id` bigint(30) NOT NULL,
  // PRIMARY KEY (`id`),
  // UNIQUE KEY `id` (`id`))
  // ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

  //CREATE TABLE `ActiveGiveaways`
  // (`guild_long_id` bigint(30) NOT NULL,
  // `message_id_long` bigint(30) NOT NULL,
  // `channel_id_long` bigint(30) NOT NULL,
  // `count_winners` varchar(255),
  // `date_end_giveaway` varchar(255),
  // `giveaway_title` varchar(255),
  // PRIMARY KEY (`guild_long_id`),
  // UNIQUE KEY `guild_long_id` (`guild_long_id`))
  // ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
  private static volatile Connection connection;
  private static volatile DataBase instance;

  //Создаем один коннект на программу
  public static Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      synchronized (DataBase.class) {
        if (connection == null || connection.isClosed()) {
          connection = DriverManager.getConnection(
              Config.getGiveawayConnection(),
              Config.getGiveawayUser(),
              Config.getGiveawayPass());
        }
      }
    }
    return connection;
  }

  public static DataBase getInstance() {
    DataBase localInstance = instance;
    if (localInstance == null) {
      synchronized (DataBase.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = localInstance = new DataBase();
        }
      }
    }
    return localInstance;
  }

  private DataBase() {}

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
      System.out.println("Recursive call");
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
  public void addMessageToDB(long guildId, long messageId, long channelId, String countWinners, String date, String title) {
    try {
      String sql = "INSERT INTO ActiveGiveaways ("
          + "guild_long_id, "
          + "message_id_long, "
          + "channel_id_long, "
          + "count_winners, "
          + "date_end_giveaway, "
          + "giveaway_title) "
          + "VALUES (?, ?, ?, ?, ?, ?)";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.setLong(1, guildId);
      preparedStatement.setLong(2, messageId);
      preparedStatement.setLong(3, channelId);
      preparedStatement.setString(4, countWinners);
      preparedStatement.setString(5, date);
      preparedStatement.setString(6, title);
      preparedStatement.execute();
      preparedStatement.close();
    } catch (SQLException e) {
      removeMessageFromDB(guildId);
      addMessageToDB(guildId, messageId, channelId, countWinners, date, title);
      e.printStackTrace();
      System.out.println("Recursive call");
    }
  }

}