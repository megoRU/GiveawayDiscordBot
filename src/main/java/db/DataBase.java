package db;

import config.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

  private final Connection conn = DriverManager
      .getConnection(Config.getCONN(), Config.getUSER(), Config.getPASS());

  public DataBase() throws SQLException {}

  //Добавление префикса
  public void addPrefixToDB(String serverId, String prefix) {
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

  //Удаление префикса
  public void removePrefixFromDB(String serverId) {
    try {
      String sql = "DELETE FROM prefixs WHERE serverId='" + serverId + "'";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      preparedStatement.execute(sql);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  //Добавляем id сообщения с реакцией
  public void insertIdMessagesWithPollEmoji(String messageId) {
    try {
      String query = "INSERT IGNORE INTO `idMessagesWithGiveawayEmoji` (idMessagesWithGiveawayEmoji) values (?)";
      PreparedStatement preparedStatement = conn.prepareStatement(query);
      preparedStatement.setString(1, messageId);
      preparedStatement.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



}