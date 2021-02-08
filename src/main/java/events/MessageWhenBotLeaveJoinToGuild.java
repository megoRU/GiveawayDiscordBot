package events;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageWhenBotLeaveJoinToGuild extends ListenerAdapter {

  //bot join msg
  @Override
  public void onGuildJoin(GuildJoinEvent event) {

    try {
      if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
        event.getGuild().getDefaultChannel().sendMessage(
            "Thanks for adding " + "**Giveaway**" + " to " + event.getGuild().getName() + "!"
                + "\nUse **!help** for a list of commands."
                + "").queue();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}