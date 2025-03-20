package main.core.events;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import lombok.AllArgsConstructor;
import main.giveaway.Exceptions;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ListUsers;
import main.model.repository.ListUsersRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;

@Service
@AllArgsConstructor
public class RerollCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(RerollCommand.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static MegoruAPI api = new MegoruAPI.Builder().build();

    private final ListUsersRepository listUsersRepository;

    public void reroll(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        var channel = Objects.requireNonNull(event.getChannel());

        event.deferReply().queue();

        String id = event.getOption("giveaway-id", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(1);

        if (id != null && id.matches("\\d+")) {
            User user = event.getUser();
            List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayIdAndCreatedUserId(Long.valueOf(id), user.getIdLong());

            if (listUsers.isEmpty()) {
                channel.retrieveMessageById(id).queue(message -> {
                    Mentions mentions = message.getMentions();
                    List<Member> members = mentions.getMembers();

                    if (!members.isEmpty()) {
                        List<Long> userListMapped = members.stream().map(Member::getUser).map(User::getIdLong).toList();
                        giveawayReroll(event, guildId, winners, userListMapped);
                    } else {
                        String noMentionsUserOnMessage = jsonParsers.getLocale("no_mentions_user_on_message", guildId);
                        event.getHook().sendMessage(noMentionsUserOnMessage).setEphemeral(true).queue();
                    }
                }, throwable -> LOGGER.error(throwable.getMessage(), throwable));
            } else {
                List<Long> userListMapped = listUsers.stream().map(ListUsers::getUserId).toList();
                giveawayReroll(event, guildId, winners, userListMapped);
            }
        } else {
            String idMustBeANumber = jsonParsers.getLocale("id_must_be_a_number", guildId);
            event.getHook().sendMessage(idMustBeANumber).setEphemeral(true).queue();
        }
    }

    private void giveawayReroll(@NotNull SlashCommandInteractionEvent event, long guildId, int winners, List<Long> userList) {
        final Set<String> uniqueWinners = new LinkedHashSet<>();

        try {
            if (userList.size() > winners) {
                Winners winnersClass = new Winners(winners, 0, userList.size() - 1);
                List<String> setWinners = api.getWinners(winnersClass);

                for (String setWinner : setWinners) {
                    uniqueWinners.add("<@" + userList.get(Integer.parseInt(setWinner)) + ">");
                }
            } else {
                uniqueWinners.add("<@" + userList.getFirst() + ">");
            }

            String winnerList = Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "")
                    .replaceAll("]", "");

            Color userColor = GiveawayUtils.getUserColor(guildId);
            String guildText = GiveawayUtils.getGuildText(guildId);

            String giftCongratulationsReroll = String.format(jsonParsers.getLocale("gift_congratulations_reroll", guildId), winnerList);
            String giftCongratulationsRerollMany = String.format(jsonParsers.getLocale("gift_congratulations_reroll_many", guildId), winnerList);

            EmbedBuilder winner = new EmbedBuilder();
            winner.setColor(userColor);

            if (guildText != null) {
                String string = guildText.replaceAll("@winner", winnerList);
                winner.setDescription(string);
            } else {
                if (winners > 1) winner.setDescription(giftCongratulationsRerollMany);
                else winner.setDescription(giftCongratulationsReroll);
            }
            event.getHook().sendMessageEmbeds(winner.build()).queue();
        } catch (Exception ex) {
            Exceptions.handle(ex, event.getHook());
        }
    }
}