package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.*;
import main.jsonparser.JSONParsers;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class PredefinedCommand {

    private final GiveawayBuilder.Builder giveawayBuilder;

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void predefined(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();
        var userId = event.getUser().getIdLong();

        if (GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.replyEmbeds(errors.build()).queue();
            return;
        }

        ChannelType channelType = event.getChannelType();
        GuildMessageChannel textChannel;

        if (channelType == ChannelType.NEWS) {
            textChannel = event.getChannel().asNewsChannel();
        } else if (channelType == ChannelType.TEXT) {
            textChannel = event.getChannel().asTextChannel();
        } else {
            event.reply("It`s not a TextChannel!").queue();
            return;
        }

        Role role = event.getOption("role", OptionMapping::getAsRole);
        String countString = event.getOption("count", OptionMapping::getAsString);
        String title = event.getOption("title", OptionMapping::getAsString);

        if (role != null) {
            if (role.getIdLong() == guildId) {
                String notificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                event.reply(notificationForThisRole).queue();
            }
        } else {
            event.reply("Role is Null").queue();
            return;
        }

        if (countString == null) {
            event.reply("Count is Null").queue();
            return;
        } else {
            if (!countString.matches("[0-9]+")) {
                event.reply("Count not a number").queue();
                return;
            }
        }

        giveawayBuilder.setTextChannelId(textChannel.getIdLong());
        giveawayBuilder.setUserIdLong(userId);
        giveawayBuilder.setGuildId(guildId);
        giveawayBuilder.setTitle(title);
        giveawayBuilder.setCountWinners(Integer.parseInt(countString));
        giveawayBuilder.setTime("20s");
        giveawayBuilder.setRoleId(role.getIdLong());
        giveawayBuilder.setForSpecificRole(true); //Тут все верно
        giveawayBuilder.setUrlImage(null);
        giveawayBuilder.setMinParticipants(2);

        Giveaway giveaway = giveawayBuilder.build();
        GiveawayRegistry.getInstance().putGift(guildId, giveaway);

        //TODO: Возможно будет проблема когда Guild слишком большая
        giveaway.startGiveaway(textChannel, true);

        event.getGuild().loadMembers()
                .onSuccess(members -> {
                    try {
                        if (!event.isAcknowledged()) {
                            String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", guildId), event.getChannel().getId());
                            event.reply(sendSlashMessage)
                                    .delay(5, TimeUnit.SECONDS)
                                    .flatMap(InteractionHook::deleteOriginal)
                                    .queue();
                        }
                    } catch (Exception ignored) {
                    }

                    if (role.getIdLong() == guildId) {
                        members.stream()
                                .map(Member::getUser)
                                .filter(user -> !user.isBot())
                                .forEach(giveaway::addUser);
                    } else {
                        members.stream()
                                .filter(member -> member.getRoles().contains(role))
                                .map(Member::getUser)
                                .filter(user -> !user.isBot())
                                .forEach(giveaway::addUser);
                    }
                })
                .onError(Throwable::printStackTrace);
    }
}