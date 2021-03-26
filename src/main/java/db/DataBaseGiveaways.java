package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBaseGiveaways {

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

  public DataBaseGiveaways() throws SQLException {}

  //Создаем таблицу когда кто-то создал Giveaway
  public void createTableWhenGiveawayStart(String guildLongId) {
    try {
      String query = "CREATE TABLE `" + guildLongId + "` (`id` bigint(30) NOT NULL, `user_long_id` bigint(30) NOT NULL, "
          + "PRIMARY KEY (`id`), "
          + "UNIQUE KEY `id` (`id`)) "
          + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
      preparedStmt.executeUpdate();
      preparedStmt.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //Удаляем таблицу когда кто-то остановил Giveaway
  public void dropTableWhenGiveawayStop(String guildLongId) {
    try {
      String query = "DROP TABLE " + guildLongId + ";";
      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
      preparedStmt.executeUpdate();
      preparedStmt.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}