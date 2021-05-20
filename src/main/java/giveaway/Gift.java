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
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import startbot.BotStart;
import threads.Giveaway;

public class Gift implements GiftHelper {

  private final JSONParsers jsonParsers = new JSONParsers();
  private final List<String> listUsers = new ArrayList<>();
  private final Map<String, String> listUsersHash = new HashMap<>();
  private final Set<String> uniqueWinners = new HashSet<>();
  private StringBuilder insertQuery = new StringBuilder();
  private final Random random = new Random();
  private final long guildId;
  private final long channelId;
  private int count;

  public Gift(long guildId, long channelId) {
    this.guildId = guildId;
    this.channelId = channelId;
  }

  protected void startGift(Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
    GiveawayRegistry.getInstance().getTitle().put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
    Instant timestamp = Instant.now();
    //Instant для timestamp
    Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));

    if (time != null) {

      start.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId())
          .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
          .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
      start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time)));
      start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
      GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
          String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time))));

      BotStart.getQueue().add(new Giveaway(
              guildId,
                      String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time)))));
    }
    if (time == null) {
      start.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId())
          .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
          .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
      GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(), "null");
    }
    GiveawayRegistry.getInstance().incrementGiveAwayCount();

    channel.sendMessage(start.build()).queue(m -> {
      GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), m.getId());
      GiveawayRegistry.getInstance().getChannelId().put(guild.getIdLong(), m.getChannel().getId());
      GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().put(guild.getIdLong(), m.getId());
      GiveawayRegistry.getInstance().getCountWinners().put(guild.getIdLong(), countWinners);
      m.addReaction(Reactions.emojiPresent).queue();
      m.addReaction(Reactions.emojiStopOne).queue();
      m.addReaction(Reactions.emojiStopTwo).queue();
      m.addReaction(Reactions.emojiStopThree).queue();
      DataBase.getInstance().addMessageToDB(guild.getIdLong(),
          m.getIdLong(),
          m.getChannel().getIdLong(),
          countWinners,
          time == null ? null : String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time))),
          GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
    });
    start.clear();
    DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());

    //Вот мы запускаем бесконечный поток.
    autoInsert();
  }

  //Добавляет пользователя в StringBuilder
  protected void addUserToPoll(User user) {
    setCount(getCount() + 1);
    listUsers.add(user.getId());
    listUsersHash.put(user.getId(), user.getId());
    addUserToInsertQuery(user.getIdLong());
  }

  private void executeMultiInsert(long guildIdLong) {
    try {
      if (!insertQuery.isEmpty()) {
        DataBase.getConnection().createStatement().execute(
            "INSERT IGNORE INTO `"
                + guildIdLong
                + "` (user_long_id) "
                + "VALUES" + insertQuery.toString());
        insertQuery = new StringBuilder();
        updateGiveawayMessage(
            GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null
                ? "TBA"
                : GiveawayRegistry.getInstance().getCountWinners().get(guildId),
            this.guildId,
            this.channelId,
            getCount());
      }
    } catch (SQLException e) {
      insertQuery = new StringBuilder();
      System.out.println("Таблица: " + guildIdLong
          + " больше не существует, скорее всего Giveaway завершился!\n"
          + "Очищаем StringBuilder!");
    }
  }

  private void addUserToInsertQuery(long userIdLong) {
    insertQuery.append(insertQuery.length() == 0 ? "" : ",").append("('").append(userIdLong).append("')");
  }

  //Автоматически отправляет в БД данные которые в буфере StringBuilder
  public void autoInsert() {
    new Timer().scheduleAtFixedRate(new TimerTask() {
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

  public void stopGift(long guildIdLong, int countWinner) {

    if (listUsers.size() < 2) {
      EmbedBuilder notEnoughUsers = new EmbedBuilder();
      notEnoughUsers.setColor(0xFF0000);
      notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
      notEnoughUsers.setDescription(jsonParsers
          .getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
      //Отправляет сообщение
      editMessage(notEnoughUsers, this.guildId, this.channelId);

      //Удаляет данные из коллекций
      clearingCollections();

      DataBase.getInstance().removeMessageFromDB(guildIdLong);
      DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

      return;
    }

    if (countWinner == 0 || countWinner >= listUsers.size()) {
      EmbedBuilder zero = new EmbedBuilder();
      zero.setColor(0xFF8000);
      zero.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));
      zero.setDescription(jsonParsers
          .getLocale("gift_Invalid_Number_Description", String.valueOf(guildIdLong))
          .replaceAll("\\{0}", String.valueOf(countWinner))
          .replaceAll("\\{1}", String.valueOf(getCount())));
      //Отправляет сообщение
      editMessage(zero, this.guildId, this.channelId);
      return;
    }
    Instant timestamp = Instant.now();
    Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
    if (countWinner > 1) {
      for (int i = 0; i < countWinner; i++) {
        int randomNumber = random.nextInt(listUsers.size());
        uniqueWinners.add("<@" + listUsers.get(randomNumber) + ">");
        listUsers.remove(randomNumber);
      }

      EmbedBuilder stopWithMoreWinner = new EmbedBuilder();
      stopWithMoreWinner.setColor(0x00FF00);
      stopWithMoreWinner.setTitle(jsonParsers.getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));
      stopWithMoreWinner.setDescription(jsonParsers
          .getLocale("gift_Giveaway_Winners", String.valueOf(guildIdLong))
          .replaceAll("\\{0}", String.valueOf(getCount()))
          + Arrays.toString(uniqueWinners.toArray())
          .replaceAll("\\[", "").replaceAll("]", ""));
      stopWithMoreWinner.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
      stopWithMoreWinner.setFooter(jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));

      //Отправляет сообщение
      editMessage(stopWithMoreWinner, this.guildId, this.channelId);

      //Удаляет данные из коллекций
      clearingCollections();

      DataBase.getInstance().removeMessageFromDB(guildIdLong);
      DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

      return;
    }

    EmbedBuilder stop = new EmbedBuilder();
    stop.setColor(0x00FF00);
    stop.setTitle(jsonParsers
        .getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));

    stop.setDescription(jsonParsers
        .getLocale("gift_Giveaway_Winner_Mention", String.valueOf(guildIdLong))
        .replaceAll("\\{0}", String.valueOf(getCount()))
        + listUsers.get(random.nextInt(listUsers.size())) + ">");
    stop.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
    stop.setFooter(jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));

    //Отправляет сообщение
    editMessage(stop, this.guildId, this.channelId);

    //Удаляет данные из коллекций
    clearingCollections();

    DataBase.getInstance().removeMessageFromDB(guildIdLong);
    DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

  }

  private void clearingCollections() {
    listUsersHash.clear();
    listUsers.clear();
    uniqueWinners.clear();
    GiveawayRegistry.getInstance().getMessageId().remove(guildId);
    GiveawayRegistry.getInstance().getChannelId().remove(guildId);
    GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().remove(guildId);
    GiveawayRegistry.getInstance().getTitle().remove(guildId);
    GiveawayRegistry.getInstance().removeGift(guildId);
    GiveawayRegistry.getInstance().decrementGiveAwayCount();
    GiveawayRegistry.getInstance().getEndGiveawayDate().remove(guildId);
    GiveawayRegistry.getInstance().getCountWinners().remove(guildId);
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

}