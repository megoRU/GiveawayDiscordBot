package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBaseForPrefix {

  private static Connection connection;

  //Создаем один коннект на программу
  public static Connection getConnection() throws SQLException {
    if (connection == null) {
      connection = DriverManager.getConnection(Config.getCONN(), Config.getUSER(), Config.getPASS());
    }
    return connection;
  }

  public DataBaseForPrefix() throws SQLException {}

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

}