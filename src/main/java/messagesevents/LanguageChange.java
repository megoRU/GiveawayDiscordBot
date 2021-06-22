package messagesevents;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class LanguageChange extends ListenerAdapter {

  private static final String LANG_RUS = "!lang rus";
  private static final String LANG_ENG = "!lang eng";
  private static final String LANG_RESET = "!lang reset";
  private static final JSONParsers jsonParsers = new JSONParsers();

  @Override
  public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {

    if (event.getAuthor().isBot()) {
      return;
    }

    if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    if (event.getMember() == null) return;

    String message = event.getMessage().getContentRaw().toLowerCase().trim();
    String[] messages = message.split(" ", 2);
    String prefix_LANG_RUS = LANG_RUS;
    String prefix_LANG_ENG = LANG_ENG;
    String prefix_LANG_RESET = LANG_RESET;

    if (BotStart.getMapPrefix().containsKey(event.getGuild().getId())) {
      prefix_LANG_RUS = BotStart.getMapPrefix().get(event.getGuild().getId()) + "lang rus";
      prefix_LANG_ENG = BotStart.getMapPrefix().get(event.getGuild().getId()) + "lang eng";
      prefix_LANG_RESET = BotStart.getMapPrefix().get(event.getGuild().getId()) + "lang reset";
    }

    if ((message.equals(prefix_LANG_RUS)
        || message.equals(prefix_LANG_RESET)
        || message.equals(prefix_LANG_ENG))
        && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
      event.getChannel()
          .sendMessage(jsonParsers.getLocale("language_change_Not_Admin", event.getGuild().getId()))
          .queue();
      return;
    }

    if (message.equals(prefix_LANG_RUS) || message.equals(prefix_LANG_ENG)) {
      BotStart.getMapLanguages().put(event.getGuild().getId(), messages[1]);

      DataBase.getInstance().addLangToDB(event.getGuild().getId(), messages[1]);

      event.getChannel()
          .sendMessage(jsonParsers
              .getLocale("language_change_lang", event.getGuild().getId())
              + "`" + messages[1].toUpperCase() + "`")
          .queue();

      return;
    }

    if (message.equals(prefix_LANG_RESET)) {
      BotStart.getMapLanguages().remove(event.getGuild().getId());

      DataBase.getInstance().removeLangFromDB(event.getGuild().getId());

      event.getChannel()
          .sendMessage(jsonParsers.getLocale("language_change_lang_reset", event.getGuild().getId()))
          .queue();
    }
  }
}
