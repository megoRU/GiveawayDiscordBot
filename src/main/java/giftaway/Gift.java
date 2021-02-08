package giftaway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import startbot.BotStart;

public class Gift {

  private final ArrayList<String> listUsers = new ArrayList<>();
  private static final HashMap<Long, String> messageId = new HashMap<>();
  private final Map<String, String> listUsersHash = new HashMap<>();
  private static final HashMap<Long, Gift> guilds = new HashMap<>();
  private final Random random = new Random();
  private final Set<String> usersWhoWinSet = new HashSet<>();
  private int count;
  private Guild guild;

  public Gift(Guild guild) {
    this.guild = guild;
  }

  public Gift() {
  }

  public void startGift(Guild guild, TextChannel channel, String guildPrefix,
      String guildPrefixStop) {
    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle("Giveaway starts");
    start.setDescription("Write to participate: `" + guildPrefix + "`"
        + "\nWrite `" + guildPrefixStop + "` to stop the giveaway"
        + "\nUsers: `" + count + "`");

    BotStart.jda.getGuildById(guild.getId())
        .getTextChannelById(channel.getId())
        .sendMessage(start.build()).queue(m -> messageId.put(guild.getIdLong(), m.getId()));
    start.clear();
  }

  public void addUserToPoll(User user, Guild guild, String guildPrefix, String guildPrefixStop,
      TextChannel channel) {
    count++;
    listUsers.add(user.getId());
    listUsersHash.put(user.getId(), user.getId());
    String avatarUrl = null;
    String avatarFromEvent = user.getAvatarUrl();
    if (avatarFromEvent != null) {
      avatarUrl = avatarFromEvent;
    }
    EmbedBuilder addUser = new EmbedBuilder();
    addUser.setColor(0x00FF00);
    addUser.setAuthor(user.getName(), null, avatarUrl);
    addUser.setDescription("You are now on the list");
    //Add user to list
    BotStart.jda.getGuildById(guild.getId())
        .getTextChannelById(channel.getId())
        .sendMessage(addUser.build()).queue(null, (exception) ->
        BotStart.jda.getGuildById(guild.getIdLong())
            .getTextChannelById(channel.getIdLong())
            .sendMessage(removeGiftExceptions(guild.getIdLong())).queue());

    EmbedBuilder edit = new EmbedBuilder();
    edit.setColor(0x00FF00);
    edit.setTitle("Giveaway");
    edit.setDescription("Write to participate: `" + guildPrefix + "`"
        + "\nWrite `" + guildPrefixStop + "` to stop the giveaway"
        + "\nUsers: `" + count + "`");

    BotStart.jda.getGuildById(guild.getId()).getTextChannelById(channel.getId())
        .editMessageById(messageId.get(guild.getIdLong()), edit.build()).queue(null, (exception) ->
        BotStart.jda.getGuildById(guild.getIdLong())
            .getTextChannelById(channel.getIdLong())
            .sendMessage(removeGiftExceptions(guild.getIdLong())).queue());
    addUser.clear();
    edit.clear();
  }

  public void stopGift(Guild guild, TextChannel channel, Integer countWinner) {
    if (listUsers.size() < 2 || listUsers.size() < countWinner) {
      BotStart.jda.getGuildById(guild.getId())
          .getTextChannelById(channel.getId())
          .sendMessage("Not enough users \nThe giveaway deleted").queue();
      listUsersHash.clear();
      listUsers.clear();
      messageId.remove(guild.getIdLong());
      removeGift(guild.getIdLong());
      return;
    }

    if (countWinner != 1) {
      for (int i = 0; i < countWinner; i++) {
        int randomNumber = random.nextInt(listUsers.size());
        usersWhoWinSet.add("<@" + listUsers.get(randomNumber) + ">");
        listUsers.remove(randomNumber);
      }

      EmbedBuilder stopWithMoreWiner = new EmbedBuilder();
      stopWithMoreWiner.setColor(0x00FF00);
      stopWithMoreWiner.setTitle("Giveaway the end");
      stopWithMoreWiner.setDescription("Winners: " + Arrays.toString(usersWhoWinSet.toArray()));
      //Add user to list
      BotStart.jda.getGuildById(guild.getId())
          .getTextChannelById(channel.getId())
          .sendMessage(stopWithMoreWiner.build()).queue();
      stopWithMoreWiner.clear();
      listUsersHash.clear();
      listUsers.clear();
      messageId.clear();
      removeGift(guild.getIdLong());
      return;
    }

    int randomWord = (int) Math.floor(Math.random() * listUsers.size());
    String winUser = listUsers.get(randomWord);
    EmbedBuilder stop = new EmbedBuilder();
    stop.setColor(0x00FF00);
    stop.setTitle("Giveaway the end");
    stop.setDescription("Winner: <@" + winUser + ">");
    //Add user to list
    BotStart.jda.getGuildById(guild.getId())
        .getTextChannelById(channel.getId())
        .sendMessage(stop.build()).queue();
    stop.clear();
    listUsersHash.clear();
    listUsers.clear();
    messageId.clear();
    removeGift(guild.getIdLong());
  }

  public String getListUsersHash(String id) {
    return listUsersHash.get(id);
  }

  protected HashMap<Long, Gift> getGuilds() {
    return guilds;
  }

  public void setGift(long guildId, Gift game) {
    guilds.put(guildId, game);
  }

  public boolean hasGift(long guildId) {
    return guilds.containsKey(guildId);
  }

  public Gift getGift(long userId) {
    return guilds.get(userId);
  }

  public void removeGift(long guildId) {
    guilds.remove(guildId);
  }

  public String removeGiftExceptions(long guildId) {
    guilds.remove(guildId);
    return "The giveaway was canceled because the bot was unable to get the ID\n" +
        "your post for editing. Please try again.";
  }

  public Guild getGuild() {
    return guild;
  }

  public void setGuild(Guild guild) {
    this.guild = guild;
  }

}
