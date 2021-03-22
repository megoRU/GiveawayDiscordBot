package startbot;

import config.Config;
import events.MessageWhenBotJoinToGuild;
import giftaway.MessageGift;
import giftaway.Reactions;
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

  public static JDA jda;
  private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
  private static final Map<String, String> mapPrefix = new HashMap<>();
//  private static final Map<String, String> idMessagesWithGiveawayEmoji = new HashMap<>();

  public void startBot() throws Exception {
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
      Connection conn = DriverManager.getConnection(Config.getCONN(), Config.getUSER(), Config.getPASS());
      Statement statement = conn.createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);
//      Statement statementSecond = conn.createStatement();

//      String sqlIdMessagesWithPollEmoji = "select * from idMessagesWithGiveawayEmoji";
//      ResultSet rsIdMessages = statementSecond.executeQuery(sqlIdMessagesWithPollEmoji);

      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }

//      while (rsIdMessages.next()) {
//        idMessagesWithGiveawayEmoji.put(
//            rsIdMessages.getString("idMessagesWithGiveawayEmoji"),
//            rsIdMessages.getString("idMessagesWithGiveawayEmoji"));
//      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static Map<String, String> getMapPrefix() {
    return mapPrefix;
  }

//  public static Map<String, String> getIdMessagesWithGiveawayEmoji() {
//    return idMessagesWithGiveawayEmoji;
//  }

  public JDA getJda() {
    return jda;
  }
}