package giveaway;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import startbot.Statcord;

public class MessageGift extends ListenerAdapter {

  private static final String GIFT_START = "!gift start";
  private static final String GIFT_START_WITH_MINUTES = "gift start\\s[0-9]{1,2}$";
  private static final String GIFT_START_TITLE = "gift start\\s.{0,255}$";
  private static final String GIFT_START_TITLE_COUNT_WITH_MINUTES = "gift start\\s.{0,255}\\s[0-9]{1,2}\\s[0-9]{1,2}$";
  private static final String GIFT_START_TITLE_WITH_MINUTES = "gift start\\s.{0,255}\\s[0-9]{1,2}$";
  private static final String GIFT_START_COUNT_WITH_MINUTES = "gift start\s[0-9]{1,2}\s[0-9]{1,2}$";
  private static final String GIFT_STOP = "!gift stop";
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

    if (!event.getGuild().getSelfMember()
        .hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    String[] messageSplit = message.split(" ", 5);
    int length = message.length();
    String messageWithOutPrefix = message.substring(1, length);

    if (messageSplit.length > 2 && messageSplit[2].equals("0")) {
      event.getChannel().sendMessage("You set `0` minutes. We took care of this and increased it by `1` minute.").queue();
      messageSplit[2] = "1";
    }

    String prefix2 = GIFT_START;
    String prefix3 = GIFT_STOP;
    String prefix4 = GIFT_COUNT;

    if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
      prefix2 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift start";
      prefix3 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift stop";
      prefix4 = BotStart.getMapPrefix().get(event.getGuild().getId()) + "gift count";
    }

    //TODO: Нужно это тестировать!
    if ((message.contains("!gift start ") && (message.length() - 11) >= 256)) {
      event.getChannel().sendMessage("The title must not be longer than 255 characters!").queue();
      return;
    }

    if (message.equals(prefix2)
        || message.equals(prefix3)
        || messageWithOutPrefix.matches(GIFT_STOP_COUNT)
        || message.equals(prefix4)
        || messageWithOutPrefix.matches(GIFT_START_TITLE)) {

      if (!messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        Statcord.commandPost(message.substring(1), event.getAuthor().getId());
      }

      if (messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        Statcord.commandPost("gift start", event.getAuthor().getId());
      }

      if (message.equals(prefix2)
          || message.equals(prefix3)
          || messageWithOutPrefix.matches(GIFT_STOP_COUNT)
          || messageWithOutPrefix.matches(GIFT_START_TITLE)) {
        long guildLongId = event.getGuild().getIdLong();

        if (!event.getMember().hasPermission(event.getChannel(), Permission.ADMINISTRATOR)
            && !event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
          event.getChannel().sendMessage("You are not Admin or you can't managed messages!").queue();
          return;
        }

        if ((message.equals(prefix2) || messageWithOutPrefix.matches(GIFT_START_TITLE))
            && GiveawayRegistry.getInstance().hasGift(guildLongId)) {
          event.getChannel().sendMessage("First you need to stop Giveaway").queue();
          return;
        }

        if ((message.equals(prefix2)
            || messageWithOutPrefix.matches(GIFT_START_TITLE)
            || messageWithOutPrefix.matches(GIFT_START_TITLE_WITH_MINUTES))
            && !GiveawayRegistry.getInstance().hasGift(guildLongId)) {
          GiveawayRegistry.getInstance().setGift(event.getGuild().getIdLong(), new Gift(event.getGuild().getIdLong()));

          if (messageSplit.length == 5 && messageWithOutPrefix.matches(GIFT_START_TITLE_COUNT_WITH_MINUTES)) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), messageSplit[2], messageSplit[4], messageSplit[3]);
            return;
          }

          if (messageSplit.length == 4 && messageWithOutPrefix.matches(GIFT_START_COUNT_WITH_MINUTES)) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), null, messageSplit[3], messageSplit[2]);
            return;
          }

          if (messageSplit.length == 4) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), messageSplit[2], null, messageSplit[3]);
            return;
          }

          if (messageSplit.length == 3 && messageWithOutPrefix.matches(GIFT_START_WITH_MINUTES)) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), null, null, messageSplit[2]);
            return;
          }

          if (messageSplit.length == 3) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), messageSplit[2], null, null);
            return;
          } else {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .startGift(event.getGuild(), event.getChannel(), null, null, null);
          }
        }

        if ((message.equals(prefix3)
            || messageWithOutPrefix.matches(GIFT_STOP_COUNT))
            && GiveawayRegistry.getInstance().hasGift(guildLongId)) {

          if (messageSplit.length == 3) {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
                .stopGift(event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                    Integer.parseInt(messageSplit[messageSplit.length - 1]));
            return;
          }
          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .stopGift(event.getGuild().getIdLong(), event.getChannel().getIdLong(), Integer.parseInt("1"));
          return;
        }
      }

      if (message.equals(prefix4) && event.getAuthor().getId().equals("250699265389625347")) {
        EmbedBuilder getCount = new EmbedBuilder();
        getCount.setTitle("Giveaway count");
        getCount.setColor(0x00FF00);
        getCount.setDescription("Active: `" + GiveawayRegistry.getInstance().getGiveAwayCount() + "`");
        event.getChannel().sendMessage(getCount.build()).queue();
        getCount.clear();
      }
    }
  }
}