package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBasePrefix {

  private static Connection connection;

  //Создаем один коннект на программу
  private static Connection getConnection() throws SQLException {
    if (connection == null) {
      connection = DriverManager.getConnection(Config.getPrefixConnection(), Config.getPrefixUser(), Config.getPrefixPass());
    }
    return connection;
  }

  public DataBasePrefix() throws SQLException {}

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
  public void addMessageToDB(long guildId, long messageId, String date) {
    try {
      String sql = "INSERT INTO ActiveGiveaways (guild_long_id, message_id_long, date_end_giveaway) VALUES (?, ?, ?)";
      PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
      preparedStatement.setLong(1, guildId);
      preparedStatement.setLong(2, messageId);
      preparedStatement.setString(3, date);
      preparedStatement.execute();
      preparedStatement.close();
    } catch (SQLException e) {
      removeMessageFromDB(guildId);
      addMessageToDB(guildId, messageId, date);
      e.printStackTrace();
    }
  }



}