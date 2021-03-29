package giveaway;

import db.DataBase;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import startbot.BotStart;

public class Gift {
  private final List<String> listUsers = new ArrayList<>();
  private final Map<String, String> listUsersHash = new HashMap<>();
  private final Set<String> uniqueWinners = new HashSet<>();
  private static final Random random = new Random();
  private long guildId;
  private int count;

  public Gift(long guildId) {
    this.guildId = guildId;
  }

  public Gift() {
  }

  public void startGift(Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
    GiveawayRegistry.getInstance().getTitle().put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
    LocalDateTime now = LocalDateTime.now();
    if (time != null) {
      GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(), now.minusHours(3).getHour()
          + ":"
          + now.plusMinutes(Long.parseLong(time)).getMinute()
          + ":"
          + now.plusMinutes(Long.parseLong(time)).getSecond());
    }
    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));

    if (time != null) {
      start.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`"
          + "\nEnds at: `" + GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) + " UTC±0`");
    }
    if (time == null) {
      start.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
    }
    GiveawayRegistry.getInstance().incrementGiveAwayCount();

    channel.sendMessage(start.build()).queue(m -> {
      GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), m.getId());
      GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().put(guild.getIdLong(), m.getId());
      m.addReaction(Reactions.emojiPresent).queue();
      try {
        DataBase dataBase = new DataBase();
        dataBase.addMessageToDB(guild.getIdLong(),
            m.getIdLong(),
            m.getChannel().getIdLong(),
            countWinners,
            time == null ? null : String.valueOf(now.plusMinutes(Long.parseLong(time))),
            GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()),
            GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()));
      } catch (SQLException throwable) {
        throwable.printStackTrace();
      }
    });
    start.clear();

    try {
      DataBase dataBase = new DataBase();
      dataBase.createTableWhenGiveawayStart(guild.getId());
    } catch (SQLException throwable) {
      throwable.printStackTrace();
    }
  }

  public void addUserToPoll(User user, Guild guild, TextChannel channel) {
    setCount(getCount() + 1);
    listUsers.add(user.getId());
    listUsersHash.put(user.getId(), user.getId());
    EmbedBuilder edit = new EmbedBuilder();
    edit.setColor(0x00FF00);
    edit.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
    if (GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) != null) {
      edit.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`"
          + "\nEnds at: `" + GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) + " UTC±0`");
    }
    if (GiveawayRegistry.getInstance().getEndGiveawayDate().get(guild.getIdLong()) == null) {
      edit.setDescription("React with :gift: to enter!" + "\nUsers: `" + getCount() + "`");
    }
    channel.editMessageById(GiveawayRegistry.getInstance().getMessageId().get(guild.getIdLong()),
        edit.build()).queue(null, (exception) -> channel
        .sendMessage(GiveawayRegistry.getInstance().removeGiftExceptions(guild.getIdLong()))
        .queue());
    edit.clear();

    try {
      DataBase dataBase = new DataBase();
      dataBase.insertUserToDB(guild.getId(), user.getIdLong());
    } catch (SQLException throwable) {
      throwable.printStackTrace();
    }

  }

  public void stopGift(long guildIdLong, long channelIdLong, Integer countWinner) {
    if (listUsers.size() < 2) {
      EmbedBuilder notEnoughUsers = new EmbedBuilder();
      notEnoughUsers.setColor(0xFF0000);
      notEnoughUsers.setTitle("Not enough users");
      notEnoughUsers.setDescription(
              """
              :x: The giveaway deleted!
              """);

      try {
        BotStart.getJda().getGuildById(guildId).getTextChannelById(channelIdLong)
            .sendMessage(notEnoughUsers.build()).queue();
        notEnoughUsers.clear();
      } catch (Exception e) {
        e.printStackTrace();
      }
      listUsersHash.clear();
      listUsers.clear();
      GiveawayRegistry.getInstance().getMessageId().remove(guildIdLong);
      GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().remove(guildIdLong);
      GiveawayRegistry.getInstance().getTitle().remove(guildIdLong);
      GiveawayRegistry.getInstance().removeGift(guildIdLong);
      GiveawayRegistry.getInstance().decrementGiveAwayCount();
      try {
        DataBase dataBase = new DataBase();
        dataBase.removeMessageFromDB(guildIdLong);
        dataBase.dropTableWhenGiveawayStop(String.valueOf(guildIdLong));
      } catch (SQLException throwable) {
        throwable.printStackTrace();
      }
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
      try {
      BotStart.getJda().getGuildById(guildId).getTextChannelById(channelIdLong)
          .sendMessage(zero.build()).queue();
      zero.clear();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
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
      try {
      BotStart.getJda().getGuildById(guildId).getTextChannelById(channelIdLong)
          .sendMessage(fewParticipants.build()).queue();
      fewParticipants.clear();
      } catch (Exception e) {
        e.printStackTrace();
      }
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
      try {
      BotStart.getJda().getGuildById(guildId).getTextChannelById(channelIdLong)
          .sendMessage(stopWithMoreWinner.build()).queue();
      stopWithMoreWinner.clear();
      } catch (Exception e) {
        e.printStackTrace();
      }
      listUsersHash.clear();
      listUsers.clear();
      GiveawayRegistry.getInstance().getMessageId().remove(guildIdLong);
      GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().remove(guildIdLong);
      GiveawayRegistry.getInstance().getTitle().remove(guildIdLong);
      GiveawayRegistry.getInstance().removeGift(guildIdLong);
      GiveawayRegistry.getInstance().decrementGiveAwayCount();
      try {
        DataBase dataBase = new DataBase();
        dataBase.removeMessageFromDB(guildIdLong);
        dataBase.dropTableWhenGiveawayStop(String.valueOf(guildIdLong));
      } catch (SQLException throwable) {
        throwable.printStackTrace();
      }
      return;
    }

    EmbedBuilder stop = new EmbedBuilder();
    stop.setColor(0x00FF00);
    stop.setTitle("Giveaway the end");
    stop.setDescription("Winner: <@" + listUsers.get(random.nextInt(listUsers.size())) + ">");
    try {
    BotStart.getJda().getGuildById(guildId).getTextChannelById(channelIdLong)
        .sendMessage(stop.build()).queue();
    stop.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
    listUsersHash.clear();
    listUsers.clear();
    GiveawayRegistry.getInstance().getMessageId().remove(guildIdLong);
    GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().remove(guildIdLong);
    GiveawayRegistry.getInstance().getTitle().remove(guildIdLong);
    GiveawayRegistry.getInstance().removeGift(guildIdLong);
    GiveawayRegistry.getInstance().decrementGiveAwayCount();
    try {
      DataBase dataBase = new DataBase();
      dataBase.removeMessageFromDB(guildIdLong);
      dataBase.dropTableWhenGiveawayStop(String.valueOf(guildIdLong));
    } catch (SQLException throwable) {
      throwable.printStackTrace();
    }
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