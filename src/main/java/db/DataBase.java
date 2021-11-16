package db;

import config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataBase {

    private static volatile DataBase dataBase;

    private DataBase() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.getGiveawayConnection(),
                Config.getGiveawayUser(),
                Config.getGiveawayPass());
    }

    public static DataBase getInstance() {
        if (dataBase == null) {
            synchronized (DataBase.class) {
                if (dataBase == null) {
                    dataBase = new DataBase();
                }
            }
        }
        return dataBase;
    }

    //Создаем таблицу когда кто-то создал Giveaway
    public void createTableWhenGiveawayStart(String guildLongId) {
        try {
            String query = "CREATE TABLE `" + guildLongId + "` (`user_long_id` bigint(30) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            getConnection().prepareStatement(query).executeUpdate();

        } catch (SQLException e) {
            try {
                getConnection();
                if (!getConnection().isClosed()) {
                    dropTableWhenGiveawayStop(guildLongId);
                    createTableWhenGiveawayStart(guildLongId);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            e.printStackTrace();
            System.out.println("Recursive call");
        }
    }

    //Удаляем таблицу когда кто-то остановил Giveaway
    public void dropTableWhenGiveawayStop(String guildLongId) {
        try {
            String query = "DROP TABLE IF EXISTS `" + guildLongId + "`;";
            getConnection().prepareStatement(query).executeUpdate();
        } catch (SQLException e) {
            try {
                getConnection();
                if (!getConnection().isClosed()) {
                    dropTableWhenGiveawayStop(guildLongId);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    //Добавление префикса
    public void addPrefixToDB(String serverId, String prefix) {
        try {
            String sql = "REPLACE INTO prefixs (serverId, prefix) VALUES (?, ?)";
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
            getConnection().prepareStatement(sql).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Добавление префикса
    public void addLangToDB(String serverId, String lang) {
        try {
            String sql = "REPLACE INTO `language` (serverId, lang) VALUES (?, ?)";
            PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
            preparedStatement.setString(1, serverId);
            preparedStatement.setString(2, lang);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            System.out.println("recursion addLangToDB");
            addLangToDB(serverId, lang);
        }
    }

    //Удаление префикса
    public void removeLangFromDB(String serverId) {
        try {
            String sql = "DELETE FROM `language` WHERE serverId='" + serverId + "'";
            getConnection().prepareStatement(sql).execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    //Удаление строки с id long message
    public void removeMessageFromDB(long guildId) {
        try {
            String sql = "DELETE FROM ActiveGiveaways WHERE guild_long_id='" + guildId + "'";
            getConnection().prepareStatement(sql).execute();
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
            try {
                getConnection().isClosed();
                if (!getConnection().isClosed()) {
                    removeMessageFromDB(guildId);
                    addMessageToDB(guildId, messageId, channelId, countWinners, date, title);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            e.printStackTrace();
            System.out.println("Recursive call");
        }
    }

}