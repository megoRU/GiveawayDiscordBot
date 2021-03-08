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

      if (!event.getGuild().getSelfMember()
          .hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
        event.getChannel().sendMessage("Bot: `" + event.getGuild().getSelfMember().getUser().getName()
            +"` don\\`t have: `Permission.MESSAGE_MANAGE` \nTo work correctly, add the specified permission to it!").queue();
        return;
      }

      //Что делает этот код...:
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
          gift.addUserToPoll(event.getMember().getUser(), event.getGuild(), event.getChannel());
        }
      }

    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

}
