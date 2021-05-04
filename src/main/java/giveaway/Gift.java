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

public class Gift {
  private final JSONParsers jsonParsers = new JSONParsers();
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

      start.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId()) + getCount() + "`");
      start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time)));
      start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
      GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
          String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(time))));
    }
    if (time == null) {
      start.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId()) + getCount() + "`");
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
      edit.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId()) + getCount() + "`");
      edit.setTimestamp(OffsetDateTime.parse(String.valueOf(GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()))));
      edit.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
    }

    if (GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) == null) {
      edit.setDescription(jsonParsers.getLocale("gift_React_With_Gift", guild.getId()) + getCount() + "`");
    }
    channel.editMessageById(GiveawayRegistry.getInstance().getMessageId().get(guild.getIdLong()),
        edit.build()).queue(null, (exception) -> channel
        .sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guild.getIdLong()))
        .queue());
    edit.clear();

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

  public void stopGift(long guildIdLong, long channelIdLong, int countWinner) {

    if (listUsers.size() < 2) {
      EmbedBuilder notEnoughUsers = new EmbedBuilder();
      notEnoughUsers.setColor(0xFF0000);
      notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
      notEnoughUsers.setDescription(jsonParsers
          .getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
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
      zero.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));
      zero.setDescription(jsonParsers
              .getLocale("gift_Invalid_Number_Description", String.valueOf(guildIdLong))
              .replaceAll("\\{0}", String.valueOf(countWinner))
              .replaceAll("\\{1}", String.valueOf(getCount())));
      //Отправляет сообщение
      sendMessage(zero, channelIdLong);
      return;
    }

    if (countWinner == listUsers.size()
        && GiveawayRegistry.getInstance().getEndGiveawayDate().get(guildIdLong) != null) {
      countWinner -= 1;

      EmbedBuilder equally = new EmbedBuilder();
      equally.setColor(0xFF8000);
      equally.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));
      equally.setDescription(jsonParsers
              .getLocale("gift_Invalid_Number_Description_Loop", String.valueOf(guildIdLong)));
      //Отправляет сообщение
      sendMessage(equally, channelIdLong);
    }

    if (countWinner >= listUsers.size()) {
      EmbedBuilder fewParticipants = new EmbedBuilder();
      fewParticipants.setColor(0xFF8000);
      fewParticipants.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));
      fewParticipants.setDescription(jsonParsers
              .getLocale("gift_Invalid_Number_Description", String.valueOf(guildIdLong))
              .replaceAll("\\{0}", String.valueOf(countWinner))
              .replaceAll("\\{1}", String.valueOf(getCount())));
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
      stopWithMoreWinner.setTitle(jsonParsers.getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));
      stopWithMoreWinner.setDescription(jsonParsers
          .getLocale("gift_Giveaway_Winners", String.valueOf(guildIdLong))
          + Arrays.toString(uniqueWinners.toArray())
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
    stop.setTitle(jsonParsers
            .getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));

    stop.setDescription(jsonParsers
            .getLocale("gift_Giveaway_Winner_Mention", String.valueOf(guildIdLong))
        + listUsers.get(random.nextInt(listUsers.size())) + ">");

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