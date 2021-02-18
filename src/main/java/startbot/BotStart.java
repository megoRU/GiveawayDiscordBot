package startbot;

import config.Config;
import events.MessageWhenBotLeaveJoinToGuild;
import giftaway.MessageGift;
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
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.discordbots.api.client.DiscordBotListAPI;

public class BotStart {

  public static JDA jda;
  private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
  public static final Map<String, String> mapPrefix = new HashMap<>();
  public static DiscordBotListAPI TOP_GG_API;

  public void startBot() throws Exception {
    jdaBuilder.setAutoReconnect(true);
    jdaBuilder.enableIntents(GatewayIntent.GUILD_MEMBERS); // also enable privileged intent
    jdaBuilder.setStatus(OnlineStatus.ONLINE);
    jdaBuilder.setActivity(Activity.playing("—> !help"));
    jdaBuilder.setBulkDeleteSplittingEnabled(false);
    jdaBuilder.addEventListeners(new MessageWhenBotLeaveJoinToGuild());
    jdaBuilder.addEventListeners(new MessageGift());
    jdaBuilder.addEventListeners(new PrefixChange());
    jdaBuilder.addEventListeners(new MessageInfoHelp());


    jda = jdaBuilder.build();
    jda.awaitReady();

    try {
      Connection conn = DriverManager.getConnection(Config.getCONN(), Config.getUSER(), Config.getPASS());
      Statement statement = conn.createStatement();
      String sql = "select * from prefixs";
      ResultSet rs = statement.executeQuery(sql);

      while (rs.next()) {
        mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }

    TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId())
            .build();
    int serverCount = (int) jda.getGuildCache().size();
    TOP_GG_API.setStats(serverCount);


    Statcord.start(jda.getSelfUser().getId(), Config.getStatcrord(), jda, true, 5);

  }

  public JDA getJda() {
    return jda;
  }
}