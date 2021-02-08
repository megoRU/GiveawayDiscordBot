package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

  private final Connection conn = DriverManager.getConnection(Config.getCONN(), Config.getUSER(), Config.getPASS());

  public DataBase() throws SQLException {
  }

  //Добавление префекса
  public void addDB(String serverId, String prefix) {
    try {
      String sql = "INSERT INTO prefixs (serverId, prefix) VALUES (?, ?)";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.setString(1, serverId);
      preparedStatement.setString(2, prefix);
      preparedStatement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Удаление префекса
  public void removeDB(String serverId) {
    try {
      String sql = "DELETE FROM prefixs WHERE serverId='" + serverId + "'";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.execute(sql);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

}

