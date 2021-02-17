package events;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageWhenBotLeaveJoinToGuild extends ListenerAdapter {

  //bot join msg
  @Override
  public void onGuildJoin(@NotNull GuildJoinEvent event) {

    try {
      if (!event.getGuild().getSelfMember()
          .hasPermission(event.getGuild().getDefaultChannel(), Permission.MESSAGE_WRITE)) {
        return;
      }
      event.getGuild().getDefaultChannel().sendMessage(
          "Thanks for adding " + "**Giveaway**" + " to " + event.getGuild().getName() + "!"
              + "\nUse **!help** for a list of commands."
              + "").queue();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}