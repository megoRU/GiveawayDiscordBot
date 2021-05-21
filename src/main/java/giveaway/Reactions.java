package giveaway;

import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.Statcord;

public class Reactions extends ListenerAdapter {

  private final JSONParsers jsonParsers = new JSONParsers();
  public static final String emojiPresent = "🎁";
  public static final String emojiStopOne = "1️⃣";
  public static final String emojiStopTwo = "2️⃣";
  public static final String emojiStopThree = "3️⃣";

  @Override
  public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {

    if (event.getUser().isBot()) {
      return;
    }

    if (GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong()) == null) {
      return;
    }

    if (!GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong()).equals(event.getMessageId())) {
      return;
    }

    if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
      return;
    }

    if (!event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
      event.getChannel().sendMessage(jsonParsers
          .getLocale("reactions_bot_dont_have_permissions", event.getGuild().getId())
          .replaceAll("\\{0}", event.getGuild().getSelfMember().getUser().getName()))
          .queue();
      return;
    }
    try {
      //Что делает этот код...:
      if (!event.getReactionEmote().isEmoji()) {
        event.getReaction().removeReaction(event.getUser()).queue();
        return;
      }
      //

      String emoji = event.getReactionEmote().getEmoji();
      boolean isThisTheEmoji = (emoji.equals(emojiPresent) || emoji.equals(emojiStopOne) || emoji.equals(emojiStopTwo) || emoji.equals(emojiStopThree));

      boolean isUserAdminOrHaveManageMessage = (event.getMember().hasPermission(event.getChannel(), Permission.ADMINISTRATOR)
          || event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE));

      if (!isThisTheEmoji) {
        event.getReaction().removeReaction(event.getUser()).queue();
      }

      long guild = event.getGuild().getIdLong();

      if (emoji.equals(emojiPresent)
          && GiveawayRegistry.getInstance().hasGift(guild)
//          && GiveawayRegistry.getInstance().getIdMessagesWithGiveawayEmoji().get(event.getGuild().getIdLong()).equals(event.getMessageId())
          && GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong()).getListUsersHash(event.getUser().getId()) == null) {
        GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong()).addUserToPoll(event.getMember().getUser());
        Statcord.commandPost("gift", event.getUser().getId());
        return;
      }

      if (isThisTheEmoji && GiveawayRegistry.getInstance().hasGift(guild) && isUserAdminOrHaveManageMessage) {

        if (emoji.equals(emojiStopOne)) {
          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .stopGift(event.getGuild().getIdLong(), 1);
          Statcord.commandPost("gift stop", event.getUser().getId());
          return;
        }

        if (emoji.equals(emojiStopTwo)) {
          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .stopGift(event.getGuild().getIdLong(), 2);
          Statcord.commandPost("gift stop 2", event.getUser().getId());
          return;
        }

        if (emoji.equals(emojiStopThree)) {
          GiveawayRegistry.getInstance().getActiveGiveaways().get(event.getGuild().getIdLong())
              .stopGift(event.getGuild().getIdLong(), 3);
          Statcord.commandPost("gift stop 3", event.getUser().getId());
        }
      }

      if (!emoji.equals(emojiPresent) && !isUserAdminOrHaveManageMessage) {
        event.getReaction().removeReaction(event.getUser()).queue();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}