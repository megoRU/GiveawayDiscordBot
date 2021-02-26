package messagesevents;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import startbot.BotStart;

public class MessageInfoHelp extends ListenerAdapter {

  private static final String HELP = "!help";

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

    if (BotStart.mapPrefix.containsKey(event.getGuild().getId())) {
      prefix = BotStart.mapPrefix.get(event.getGuild().getId()) + "help";
      p = BotStart.mapPrefix.get(event.getGuild().getId());
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
      info.addField("Prefix:",
              """
              `*prefix <symbol>` - Changes the prefix.
              `*prefix reset` - Reset the prefix.
              """
          , false);

      info.addField("Giveaway:", "`"
          + p + "gift start` - Run Giveaway.\n`"
          + p + "gift stop` - Stop Giveaway.\n`"
          + p + "gift stop <number>` - Stop Giveaway with more winners.\n", false);

      info.addField("Links:", """
          :zap: [megoru.ru](https://megoru.ru)
          :robot: [Add me to other guilds](https://discord.com/oauth2/authorize?client_id=808277484524011531&scope=bot&permissions=3072)
          :boom: [Vote for this bot](https://top.gg/bot/808277484524011531/vote)""", false);

      info.addField("Bot creator", ":tools: [mego](https://steamcommunity.com/id/megoRU)", false);
      info.addField("Support", ":helmet_with_cross: [Discord server](https://discord.com/invite/UrWG3R683d)", false);
      event.getChannel().sendMessage("I sent you a private message").delay(5, TimeUnit.SECONDS)
          .flatMap(Message::delete).queue();

      event.getMember().getUser().openPrivateChannel()
          .flatMap(m -> event.getMember().getUser().openPrivateChannel())
          .flatMap(channel -> channel.sendMessage(info.build()))
          .queue(null, error -> event.getChannel().sendMessage("Failed to send message. "
              + "Maybe I'm on your blacklist!").queue());
    }
  }
}
