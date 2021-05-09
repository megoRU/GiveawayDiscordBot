package messagesevents;

import giveaway.GiftHelper;
import java.util.concurrent.TimeUnit;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class MessageInfoHelp extends ListenerAdapter {

  private static final String HELP = "!help";
  private final JSONParsers jsonParsers = new JSONParsers();

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    if (event.isFromType(ChannelType.PRIVATE) && event.getMessage().getContentDisplay().trim().equals("!help")) {
      buildMessage(ChannelType.PRIVATE, event);
      return;
    }

    if (!event.isFromType(ChannelType.PRIVATE)) {
      buildMessage(ChannelType.TEXT, event);
    }
  }

  private void buildMessage(ChannelType channelType, @NotNull MessageReceivedEvent event) {
    String p = "!";
    String prefix = HELP;
    if (!channelType.equals(ChannelType.PRIVATE)) {

      if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
        p = BotStart.getMapPrefix().get(event.getGuild().getId());
        prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "help";
      }
    }

    if (event.getMessage().getContentDisplay().trim().equals(prefix)) {
      String avatarUrl = null;
      String avatarFromEvent = event.getMessage().getAuthor().getAvatarUrl();
      if (avatarFromEvent == null) {
        avatarUrl = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
      }
      if (avatarFromEvent != null) {
        avatarUrl = avatarFromEvent;
      }
      String guildIdLong = !channelType.equals(ChannelType.PRIVATE) ? event.getGuild().getId() : "0000000000000";

      EmbedBuilder info = new EmbedBuilder();
      info.setColor(0xa224db);
      info.setAuthor(event.getAuthor().getName(), null, avatarUrl);
      info.addField(jsonParsers.getLocale("messages_events_Prefix", guildIdLong),
          jsonParsers.getLocale("messages_events_Changes_Prefix", guildIdLong) +
              jsonParsers.getLocale("messages_events_Reset_Prefix", guildIdLong), false);

      info.addField(jsonParsers.getLocale("messages_events_Language_Title", guildIdLong), "`"
              + p + jsonParsers.getLocale("messages_events_Language", guildIdLong) + "`"
              + p + jsonParsers.getLocale("messages_events_Language_Reset", guildIdLong)
          , false);

      info.addField("Giveaway:", "`"
          + p + jsonParsers.getLocale("messages_events_Start_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Start_Text_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Start_Text_Time_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Start_Text_Time_Count_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Start_Time_Count_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Stop_Giveaway", guildIdLong)
          + p + jsonParsers.getLocale("messages_events_Stop_Number_Giveaway", guildIdLong), false);

      info.addField(jsonParsers.getLocale("messages_events_Links", guildIdLong),
          jsonParsers.getLocale("messages_events_Site", guildIdLong) +
              jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", guildIdLong) +
              jsonParsers.getLocale("messages_events_Vote_For_This_Bot", guildIdLong), false);

      info.addField(
          jsonParsers.getLocale("messages_events_Bot_Creator", guildIdLong),
          jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", guildIdLong), false);

      info.addField(
          jsonParsers.getLocale("messages_events_Support", guildIdLong),
          jsonParsers.getLocale("messages_events_Support_Url_Discord", guildIdLong), false);

      switch (channelType) {
        case TEXT -> {
          event.getChannel().sendMessage(jsonParsers.getLocale("messages_events_Send_Private_Message",
              event.getGuild().getId())).delay(5, TimeUnit.SECONDS)
              .flatMap(Message::delete).queue();
          event.getMember().getUser().openPrivateChannel()
              .flatMap(m -> event.getMember().getUser().openPrivateChannel())
              .flatMap(channel -> channel.sendMessage(info.build()))
              .queue(null, error -> event.getChannel()
                  .sendMessage(jsonParsers.getLocale("messages_events_Failed_To_Send_Message", event.getGuild().getId())).queue());
        }
        case PRIVATE -> {
          GiftHelper.sendMessage(info, event);
          info.clear();
        }
      }
    }
  }
}