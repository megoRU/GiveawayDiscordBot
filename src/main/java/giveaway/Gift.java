package giveaway;

import db.DataBase;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import startbot.BotStart;

public class Gift {
  private final List<String> listUsers = new ArrayList<>();
  private final Map<String, String> listUsersHash = new HashMap<>();
  private final Set<String> uniqueWinners = new HashSet<>();
  private StringBuilder insertQuery = new StringBuilder();
  private final Random random = new Random();
  private long guildId;
  private int count;

  public Gift(long guildId) {
    this.guildId = guildId;
  }

  public Gift() {}

  public void startGift(Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
    GiveawayRegistry.getInstance().getTitle().put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
    Instant timestamp = Instant.now();
    //Instant для timestamp
    Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));

    if (time != null) {
      start.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
      start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time)));
      start.setFooter("Ends at:");
      GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
          String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time))));
    }
    if (time == null) {
      start.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
    }
    GiveawayRegistry.getInstance().incrementGiveAwayCount();

    channel.sendMessage(start.build()).queue(m -> {
      GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), m.getId());
      GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().put(guild.getIdLong(), m.getId());
      m.addReaction(Reactions.emojiPresent).queue();
      DataBase.getInstance().addMessageToDB(guild.getIdLong(),
          m.getIdLong(),
          m.getChannel().getIdLong(),
          countWinners,
          time == null ? null : String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time))),
          GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
    });
    start.clear();
    //Вот мы запускаем бесконечный поток.
    autoInsert();

    DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());
  }

  public void addUserToPoll(User user, Guild guild, TextChannel channel) {
    setCount(getCount() + 1);
    listUsers.add(user.getId());
    listUsersHash.put(user.getId(), user.getId());
    EmbedBuilder edit = new EmbedBuilder();
    edit.setColor(0x00FF00);
    edit.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));

    if (GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) != null) {
      edit.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
      edit.setTimestamp(OffsetDateTime.parse(String.valueOf(GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()))));
      edit.setFooter("Ends at:");
    }

    if (GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) == null) {
      edit.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
    }
    channel.editMessageById(GiveawayRegistry.getInstance().getMessageId().get(guild.getIdLong()),
        edit.build()).queue(null, (exception) -> channel
        .sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guild.getIdLong()))
        .queue());
    edit.clear();

    addUserToInsertQuery(user.getIdLong());

  }

  public void executeMultiInsert(long guildIdLong) throws SQLException {
    String sql = "INSERT IGNORE INTO `"
        + guildIdLong
        + "` (user_long_id) "
        + "VALUES" + insertQuery.toString();
    if (!insertQuery.isEmpty()) {
      DataBase.getConnection().createStatement().execute(sql);
      insertQuery = new StringBuilder(); //Нужно протестировать
    }
  }

  public void addUserToInsertQuery(long userIdLong) {
    insertQuery.append(insertQuery.length() == 0 ? "" : ",").append("('").append(userIdLong).append("')");
  }

  //Автоматически отправляет в БД данные которые в буфере StringBuilder
  public void autoInsert() {
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      public void run() throws NullPointerException {
        try {
          executeMultiInsert(guildId);
        } catch (Exception e) {
          Thread.currentThread().interrupt();
          e.printStackTrace();
        }
      }
    }, 1, 5000);
  }

  public void stopGift(long guildIdLong, long channelIdLong, int countWinner) {

    if (listUsers.size() < 2) {
      EmbedBuilder notEnoughUsers = new EmbedBuilder();
      notEnoughUsers.setColor(0xFF0000);
      notEnoughUsers.setTitle("Not enough users");
      notEnoughUsers.setDescription(
              """
              :x: The giveaway deleted!
              """);
      //Отправляет сообщение
      sendMessage(notEnoughUsers, channelIdLong);

      //Удаляет данные из коллекций
      clearingCollections();

      DataBase.getInstance().removeMessageFromDB(guildIdLong);
      DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

      return;
    }

    if (countWinner == 0) {
      EmbedBuilder zero = new EmbedBuilder();
      zero.setColor(0xFF8000);
      zero.setTitle(":warning: Invalid number");
      zero.setDescription(
              """
              The number of winners must be greater than zero!
              """ +
              "You entered a number: `" + countWinner + "`\n" +
              "Number of participants: `" + getCount() + "`\n" +
              """ 
              This action did not cause the deletion: **Giveaway**!
              """);
      //Отправляет сообщение
      sendMessage(zero, channelIdLong);
      return;
    }

    if (countWinner == listUsers.size()
        && GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildIdLong) != null) {
      countWinner -= 1;

      EmbedBuilder equally = new EmbedBuilder();
      equally.setColor(0xFF8000);
      equally.setTitle(":warning: Invalid number");
      equally.setDescription(
          """
          In order not to get loop, we had to reduce the number
          of winners by one. Since their number was equal to the participants
          """);
      //Отправляет сообщение
      sendMessage(equally, channelIdLong);
    }

    if (countWinner >= listUsers.size()) {
      EmbedBuilder fewParticipants = new EmbedBuilder();
      fewParticipants.setColor(0xFF8000);
      fewParticipants.setTitle(":warning: Invalid number");
      fewParticipants.setDescription(
              """
              The number of winners must be less than the number of participants!
              """ +
              "You entered a number: `" + countWinner + "`\n" +
              "Number of participants: `" + getCount() + "`\n" +
              """ 
              This action did not cause the deletion: **Giveaway**!
              """
      );
      //Отправляет сообщение
      sendMessage(fewParticipants, channelIdLong);

      return;
    }

    if (countWinner > 1) {
      for (int i = 0; i < countWinner; i++) {
        int randomNumber = random.nextInt(listUsers.size());
        uniqueWinners.add("<@" + listUsers.get(randomNumber) + ">");
        listUsers.remove(randomNumber);
      }

      EmbedBuilder stopWithMoreWinner = new EmbedBuilder();
      stopWithMoreWinner.setColor(0x00FF00);
      stopWithMoreWinner.setTitle("Giveaway the end");
      stopWithMoreWinner.setDescription("Winners: " + Arrays.toString(uniqueWinners.toArray())
          .replaceAll("\\[", "").replaceAll("]", ""));

      //Отправляет сообщение
      sendMessage(stopWithMoreWinner, channelIdLong);

      //Удаляет данные из коллекций
      clearingCollections();

      DataBase.getInstance().removeMessageFromDB(guildIdLong);
      DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

      return;
    }

    EmbedBuilder stop = new EmbedBuilder();
    stop.setColor(0x00FF00);
    stop.setTitle("Giveaway the end");
    stop.setDescription("Winner: <@" + listUsers.get(random.nextInt(listUsers.size())) + ">");

    //Отправляет сообщение
    sendMessage(stop, channelIdLong);

    //Удаляет данные из коллекций
    clearingCollections();

    DataBase.getInstance().removeMessageFromDB(guildIdLong);
    DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

  }

  private void sendMessage(EmbedBuilder embedBuilder, long channelIdLong) {
    try {
      BotStart.getJda()
          .getGuildById(guildId)
          .getTextChannelById(channelIdLong)
          .sendMessage(embedBuilder.build())
          .queue();
      embedBuilder.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void clearingCollections() {
    listUsersHash.clear();
    listUsers.clear();
    GiveawayRegistry.getInstance().getMessageId().remove(guildId);
    GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().remove(guildId);
    GiveawayRegistry.getInstance().getTitle().remove(guildId);
    GiveawayRegistry.getInstance().removeGift(guildId);
    GiveawayRegistry.getInstance().decrementGiveAwayCount();
    GiveawayRegistry.getInstance().getEndGiveawayDate().remove(guildId);
  }

  public String getListUsersHash(String id) {
    return listUsersHash.get(id);
  }

  public Map<String, String> getListUsersHash() {
    return listUsersHash;
  }

  public long getGuild() {
    return guildId;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public List<String> getListUsers() {
    return listUsers;
  }

  public StringBuilder getInsertQuery() {
    return insertQuery;
  }

}