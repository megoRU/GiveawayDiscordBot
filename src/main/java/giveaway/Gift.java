package giveaway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Gift {

  private final List<String> listUsers = new ArrayList<>();
  private final Map<String, String> listUsersHash = new HashMap<>();
  private final Set<String> uniqueWinners = new HashSet<>();

  private static final AtomicInteger giveawayCount = new AtomicInteger(0);
  private static final Map<Long, String> messageId = new HashMap<>();
  private static final Map<Long, String> idMessagesWithGiveawayEmoji = new HashMap<>();
  private static final Map<Long, String> title = new HashMap<>();
  private static final Random random = new Random();

  private Guild guild;
  private int count;

  public Gift(Guild guild) {
    this.guild = guild;
  }

  public Gift() {}

  public void startGift(Guild guild, TextChannel channel, String newTitle) {
    title.put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
    EmbedBuilder start = new EmbedBuilder();
    start.setColor(0x00FF00);
    start.setTitle(title.get(guild.getIdLong()));
    start.setDescription("React with :gift: to enter!" + "\nUsers: `" + count + "`");
    incrementGiveAwayCount();
    channel.sendMessage(start.build()).queue(m -> {
      messageId.put(guild.getIdLong(), m.getId());
      idMessagesWithGiveawayEmoji.put(guild.getIdLong(), m.getId());
      m.addReaction(Reactions.emojiPresent).queue();
    });
    start.clear();
  }

  public void addUserToPoll(User user, Guild guild, TextChannel channel) {
    count++;
    listUsers.add(user.getId());
    listUsersHash.put(user.getId(), user.getId());
    EmbedBuilder edit = new EmbedBuilder();
    edit.setColor(0x00FF00);
    edit.setTitle(title.get(guild.getIdLong()));
    edit.setDescription("React with :gift: to enter!"
        + "\nUsers: `" + count + "`");
    GiveawayRegistry giveawayRegistry = new GiveawayRegistry();
    channel.editMessageById(messageId.get(guild.getIdLong()), edit.build())
        .queue(null, (exception) -> channel
                .sendMessage(giveawayRegistry.removeGiftExceptions(guild.getIdLong())).queue());
    edit.clear();
  }

  public void stopGift(Guild guild, TextChannel channel, Integer countWinner) {
    GiveawayRegistry giveawayRegistry = new GiveawayRegistry();

    if (listUsers.size() < 2) {
      EmbedBuilder notEnoughUsers = new EmbedBuilder();
      notEnoughUsers.setColor(0xFF0000);
      notEnoughUsers.setTitle("Not enough users");
      notEnoughUsers.setDescription(
              """
              :x: The giveaway deleted!
              """);
      channel.sendMessage(notEnoughUsers.build()).queue();
      notEnoughUsers.clear();
      listUsersHash.clear();
      listUsers.clear();
      messageId.remove(guild.getIdLong());
      idMessagesWithGiveawayEmoji.remove(guild.getIdLong());
      title.remove(guild.getIdLong());
      giveawayRegistry.removeGift(guild.getIdLong());
      decrementGiveAwayCount();
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
              "Number of participants: `" + count + "`\n" +
              """ 
              This action did not cause the deletion: **Giveaway**!
              """);
      channel.sendMessage(zero.build()).queue();
      zero.clear();
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
              "Number of participants: `" + count + "`\n" +
              """ 
              This action did not cause the deletion: **Giveaway**!
              """
      );
      channel.sendMessage(fewParticipants.build()).queue();
      fewParticipants.clear();
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
      channel.sendMessage(stopWithMoreWinner.build()).queue();
      stopWithMoreWinner.clear();
      listUsersHash.clear();
      listUsers.clear();
      messageId.clear();
      idMessagesWithGiveawayEmoji.remove(guild.getIdLong());
      giveawayRegistry.removeGift(guild.getIdLong());
      title.remove(guild.getIdLong());
      decrementGiveAwayCount();
      return;
    }

    EmbedBuilder stop = new EmbedBuilder();
    stop.setColor(0x00FF00);
    stop.setTitle("Giveaway the end");
    stop.setDescription("Winner: <@" + listUsers.get(random.nextInt(listUsers.size())) + ">");
    channel.sendMessage(stop.build()).queue();
    stop.clear();
    listUsersHash.clear();
    listUsers.clear();
    messageId.clear();
    idMessagesWithGiveawayEmoji.remove(guild.getIdLong());
    giveawayRegistry.removeGift(guild.getIdLong());
    decrementGiveAwayCount();
  }

  public String getListUsersHash(String id) {
    return listUsersHash.get(id);
  }

  public static Map<Long, String> getIdMessagesWithGiveawayEmoji() {
    return idMessagesWithGiveawayEmoji;
  }

  public Guild getGuild() {
    return guild;
  }

  public synchronized void incrementGiveAwayCount() {
    giveawayCount.getAndIncrement();
  }

  public synchronized void decrementGiveAwayCount() {
    giveawayCount.decrementAndGet();
  }

  public synchronized Integer getGiveAwayCount() {
    return giveawayCount.get();
  }

}