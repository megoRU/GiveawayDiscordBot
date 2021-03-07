package giftaway;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class Reactions extends ListenerAdapter {

  private static final String emojiPresent = "\uD83C\uDF81";

  @Override
  public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
    try {
      if (event.getUser().isBot()) {
        return;
      }

      if (!event.getGuild().getSelfMember()
          .hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
        return;
      }

      String prefix = BotStart.getMapPrefix().get(event.getGuild().getId()) == null ? "!"
          : BotStart.getMapPrefix().get(event.getGuild().getId());

      if (!event.getGuild().getSelfMember()
          .hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
        event.getChannel().sendMessage("Bot don\\`t have: `Permission.MESSAGE_MANAGE`").queue();
        return;
      }

      //Что делает этот код...:
      System.out.println(event.getReactionEmote().isEmoji());
      if (!event.getReactionEmote().isEmoji()) {
        event.getReaction().removeReaction(event.getUser()).queue();
        return;
      }
      //

      String emoji = event.getReactionEmote().getEmoji();

      if (!(emoji.equals(emojiPresent))
          && BotStart.getIdMessagesWithGiveawayEmoji().get(event.getMessageId()) != null) {
        event.getReaction().removeReaction(event.getUser()).queue();
      }

      long guild = event.getGuild().getIdLong();
      Gift gift;
      gift = new Gift();

      if (emoji.equals(emojiPresent)
          && BotStart.getIdMessagesWithGiveawayEmoji().get(event.getMessageId()) != null
          && gift.hasGift(guild)) {
        gift = gift.getGift(event.getGuild().getIdLong());
        if (gift.getListUsersHash(event.getUser().getId()) == null) {
          gift.addUserToPoll(event.getMember().getUser(), event.getGuild(), prefix, event.getChannel());
        }
      }

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

}
