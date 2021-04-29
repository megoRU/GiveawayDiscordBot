package messagesevents;

import java.util.concurrent.TimeUnit;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import startbot.BotStart;

public class MessageInfoHelp extends ListenerAdapter {

  private static final String HELP = "!help";
  private final JSONParsers jsonParsers = new JSONParsers();

  @Override
  public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    if (!event.getGuild().getSelfMember()
        .hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }
    String message = event.getMessage().getContentRaw().toLowerCase();
    String prefix = HELP;

    String p = "!";

    if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
      prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) + "help";
      p = BotStart.getMapPrefix().get(event.getGuild().getId());
    }

    if (message.equals(prefix)) {
      String avatarUrl = null;
      String avatarFromEvent = event.getMessage().getAuthor().getAvatarUrl();
      if (avatarFromEvent == null) {
        avatarUrl = "https://cdn.discordapp.com/avatars/754093698681274369/dc4b416065569253bc6323efb6296703.png";
      }
      if (avatarFromEvent != null) {
        avatarUrl = avatarFromEvent;
      }
      EmbedBuilder info = new EmbedBuilder();
      info.setColor(0xa224db);
      info.setAuthor(event.getAuthor().getName(), null, avatarUrl);
      info.addField(
          jsonParsers.getLocale("messages_events_Prefix", event.getGuild().getId()),
          jsonParsers.getLocale("messages_events_Changes_Prefix", event.getGuild().getId()) +
              jsonParsers.getLocale("messages_events_Reset_Prefix", event.getGuild().getId())
          , false);

      info.addField(
          jsonParsers.getLocale("messages_events_Language_Title", event.getGuild().getId()),
          "`"
            + p + jsonParsers.getLocale("messages_events_Language", event.getGuild().getId())
              + "`"
            + p + jsonParsers.getLocale("messages_events_Language_Reset", event.getGuild().getId())
          , false);

      info.addField("Giveaway:", "`"
          + p + jsonParsers.getLocale("messages_events_Start_Giveaway", event.getGuild().getId())
          + p + jsonParsers.getLocale("messages_events_Start_Text_Giveaway", event.getGuild().getId())
          + p + jsonParsers.getLocale("messages_events_Stop_Giveaway", event.getGuild().getId())
          + p + jsonParsers.getLocale("messages_events_Stop_Number_Giveaway", event.getGuild().getId()), false);

      info.addField(jsonParsers.getLocale("messages_events_Links", event.getGuild().getId()),
          jsonParsers.getLocale("messages_events_Site", event.getGuild().getId()) +
              jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", event.getGuild().getId()) +
              jsonParsers.getLocale("messages_events_Vote_For_This_Bot", event.getGuild().getId()), false);

      info.addField(jsonParsers.getLocale("messages_events_Bot_Creator", event.getGuild().getId()),
          jsonParsers.getLocale("messages_events_Bot_Creator_Url_Steam", event.getGuild().getId()), false);

      info.addField(jsonParsers.getLocale("messages_events_Support", event.getGuild().getId()),
          jsonParsers.getLocale("messages_events_Support_Url_Discord", event.getGuild().getId()), false);

      event.getChannel().sendMessage(jsonParsers.getLocale("messages_events_Send_Private_Message",
          event.getGuild().getId())).delay(5, TimeUnit.SECONDS)
          .flatMap(Message::delete).queue();

      event.getMember().getUser().openPrivateChannel()
          .flatMap(m -> event.getMember().getUser().openPrivateChannel())
          .flatMap(channel -> channel.sendMessage(info.build()))
          .queue(null, error -> event.getChannel()
              .sendMessage(jsonParsers
                  .getLocale("messages_events_Failed_To_Send_Message", event.getGuild().getId())).queue());
    }
  }
}
