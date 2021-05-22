package giveaway;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import startbot.Statcord;

public class MessageGift extends ListenerAdapter {

  private final JSONParsers jsonParsers = new JSONParsers();
  private static final String GIFT_START = "!gift start";
  private static final String GIFT_START_WITHOUT_PREFIX = "gift start";
  private static final String GIFT_START_WITH_MINUTES = "gift start\\s[0-9]{1,2}[mмhчdд]$";
  private static final String GIFT_START_TITLE = "gift start\\s.{0,255}$";
  private static final String GIFT_START_TITLE_MINUTES_WITH_COUNT = "gift start\\s.{0,255}\\s[0-9]{1,2}[mмhчdд]\\s[0-9]{1,2}$";
  private static final String GIFT_START_TITLE_WITH_MINUTES = "gift start\\s.{0,255}\\s[0-9]{1,2}[mмhчdд]$";
  private static final String GIFT_START_COUNT_WITH_MINUTES = "gift start\\s[0-9]{1,2}[mмhчdд]\\s[0-9]{1,2}$";
  private static final String GIFT_STOP = "gift stop";
  private static final String GIFT_STOP_COUNT = "gift stop\\s[0-9]+";
  private static final String GIFT_COUNT = "!gift count";

  @Override
  public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    String message = event.getMessage().getContentRaw().toLowerCase().trim();

    if (message.equals("")) {
      return;
    }

    if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    String[] messageSplit = message.split(" ");
    int length = message.length();
    String messageWithOutPrefix = message.substring(1, length);

    String prefix_GIFT_START = GIFT_START;
    String prefix_GIFT_COUNT = GIFT_COUNT;

    if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
      prefix_GIFT_START = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift start";
      prefix_GIFT_COUNT = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift count";
    }

    //TODO: Нужно это тестировать!
    if ((message.contains(prefix_GIFT_START + " ") && (message.length() - 11) >= 256)) {
      event.getChannel()
          .sendMessage(jsonParsers.getLocale("message_gift_Long_Title", event.getGuild().getId()))
          .queue();
      return;
    }

    if (messageWithOutPrefix.matches(GIFT_STOP) || messageWithOutPrefix.matches(GIFT_STOP_COUNT)) {
      Statcord.commandPost("gift stop", event.getAuthor().getId());
    }

    if (messageWithOutPrefix.matches(GIFT_START_TITLE) || messageWithOutPrefix.matches(GIFT_START_WITHOUT_PREFIX)) {
      Statcord.commandPost("gift start", event.getAuthor().getId());
    }

    long guildLongId = event.getGuild().getIdLong();
    boolean isMessageMatches = (messageWithOutPrefix.matches(GIFT_START_TITLE) || messageWithOutPrefix.matches(GIFT_START_WITHOUT_PREFIX));

    if (!event.getMember().hasPermission(event.getChannel(), Permission.ADMINISTRATOR)
        && !event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)
        && (messageWithOutPrefix.matches(GIFT_START_TITLE)
        || messageWithOutPrefix.matches(GIFT_START_WITHOUT_PREFIX)
        || messageWithOutPrefix.matches(GIFT_STOP)
        || messageWithOutPrefix.matches(GIFT_STOP_COUNT))) {
      event.getChannel()
          .sendMessage(jsonParsers.getLocale("message_gift_Not_Admin", event.getGuild().getId()))
          .queue();
      return;
    }

    if (isMessageMatches && GiveawayRegistry.getInstance().hasGift(guildLongId)) {
      event.getChannel()
          .sendMessage(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId()))
          .queue();
      return;
    }

    //GIFT START
    if (isMessageMatches && !GiveawayRegistry.getInstance().hasGift(guildLongId)) {

      if (messageSplit.length > 2 && messageSplit[2].equals("0")) {
        event.getChannel()
            .sendMessage(jsonParsers.getLocale("message_gift_Set_Zero_Minutes", event.getGuild().getId()))
            .queue();
        messageSplit[2] = "1";
      }

      GiveawayRegistry.getInstance().setGift(event.getGuild().getIdLong(), new Gift(event.getGuild().getIdLong(), event.getChannel().getIdLong()));

      if (messageWithOutPrefix.matches(GIFT_START_TITLE_MINUTES_WITH_COUNT)) {
        int len = messageSplit[messageSplit.length - 1].length() + messageSplit[messageSplit.length - 2].length();
        GiveawayRegistry.getInstance()
            .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(
            event.getGuild(),
            event.getChannel(),
            event.getMessage().getContentDisplay().substring(12, message.length() - len - 1),
            messageSplit[messageSplit.length - 1],
            messageSplit[messageSplit.length - 2]);
        return;
      }

      if (messageWithOutPrefix.matches(GIFT_START_COUNT_WITH_MINUTES)) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
            .startGift(event.getGuild(), event.getChannel(), null, messageSplit[3], messageSplit[2]);
        return;
      }

      if (messageWithOutPrefix.matches(GIFT_START_TITLE_WITH_MINUTES)) {
        GiveawayRegistry.getInstance()
            .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(
            event.getGuild(),
            event.getChannel(),
            event.getMessage().getContentDisplay().substring(12, message.length() - 3),
            null,
            messageSplit[messageSplit.length - 1]);
        return;
      }

      if (messageWithOutPrefix.matches(GIFT_START_WITH_MINUTES)) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
            .startGift(event.getGuild(), event.getChannel(), null, null, messageSplit[2]);
        return;
      }

      if (messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
            .startGift(
                event.getGuild(), event.getChannel(),
                event.getMessage().getContentDisplay().substring(12, message.length()), null, null);
        return;
      } else {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
            .startGift(event.getGuild(), event.getChannel(), null, null, null);
      }
    }

    //GIFT STOP
    if ((messageWithOutPrefix.matches(GIFT_STOP)
        || messageWithOutPrefix.matches(GIFT_STOP_COUNT))
        && GiveawayRegistry.getInstance().hasGift(guildLongId)) {

      if (messageWithOutPrefix.matches(GIFT_STOP_COUNT)) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
            .stopGift(event.getGuild().getIdLong(), Integer.parseInt(messageSplit[messageSplit.length - 1]));
        return;
      }
      GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
          .stopGift(event.getGuild().getIdLong(), Integer.parseInt("1"));
      return;
    }

    if (message.equals(prefix_GIFT_COUNT) && event.getAuthor().getId().equals("250699265389625347")) {
      EmbedBuilder getCount = new EmbedBuilder();
      getCount.setTitle("Giveaway count");
      getCount.setColor(0x00FF00);
      getCount.setDescription("Active: `" + GiveawayRegistry.getInstance().getGiveAwayCount() + "`");
      event.getChannel().sendMessage(getCount.build()).queue();
      getCount.clear();

    }
  }
}