package main.core.events;

import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@Component
public class RecoveryGiveaway {

    private final ActiveGiveawayRepository activeGiveawayRepository;

    private final static Logger LOGGER = LoggerFactory.getLogger(RecoveryGiveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void recoveryGiveaway(@NotNull SlashCommandInteractionEvent event) {
        var messageId = event.getOption("message-id", OptionMapping::getAsLong);
        long userId = event.getUser().getIdLong();

        if (messageId == null) {
            event.reply("You need to provide a message ID!").queue();
            return;
        }

        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        MessageChannelUnion channel = event.getChannel();

        channel.retrieveMessageById(messageId).queue(message -> {
            if (message.getEmbeds().isEmpty()) {
                event.reply("Embed не найден.").setEphemeral(true).queue();
                return;
            }

            MessageEmbed embed = message.getEmbeds().getFirst();

            String title = embed.getTitle();
            String description = embed.getDescription();

            if (description == null) {
                event.reply("Описание embed отсутствует.").setEphemeral(true).queue();
                return;
            }

            String language = JSONParsers.getLanguage(guildId);

            String giftHosted = jsonParsers.getLocale("gift_hosted", guildId)
                    .replace("\n", "")
                    .replace("<@%s>", "")
                    .trim();

            String minParticipantsText = jsonParsers.getLocale("list_menu_participants", guildId).trim();

            String winnerWord = language.equals("eng")
                    ? "Winner(?:s)?"
                    : "Победител(?:ь|я|ей)";

            Long roleId = null;
            boolean isForSpecificRole = false;
            Long createdUserId = null;
            Instant endDate = null;
            Integer minParticipants = null;

            Matcher roleMatcher = Pattern.compile("<@&(\\d+)>").matcher(description);
            if (roleMatcher.find()) {
                roleId = Long.parseLong(roleMatcher.group(1));
                isForSpecificRole = true;
            }

            Matcher creatorMatcher = Pattern.compile(Pattern.quote(giftHosted) + "\\s*<@(\\d+)>").matcher(description);
            if (creatorMatcher.find()) {
                createdUserId = Long.parseLong(creatorMatcher.group(1));
            }

            Matcher endMatcher = Pattern.compile("<t:(\\d+):[RFf]>").matcher(description);
            if (endMatcher.find()) {
                endDate = Instant.ofEpochSecond(Long.parseLong(endMatcher.group(1)));
            }

            Matcher minMatcher = Pattern.compile(Pattern.quote(minParticipantsText) + "\\s*(\\d+)").matcher(description);
            if (minMatcher.find()) {
                minParticipants = Integer.parseInt(minMatcher.group(1));
            }

            Matcher winnersMatcher = Pattern.compile("(\\d+)\\s+" + winnerWord).matcher(description);
            int winners = winnersMatcher.find()
                    ? Integer.parseInt(winnersMatcher.group(1))
                    : 1;

            String imageUrl = embed.getImage() == null ? null : embed.getImage().getUrl();

            ActiveGiveaways giveaway = new ActiveGiveaways();
            giveaway.setMessageId(message.getIdLong());
            giveaway.setChannelId(channel.getIdLong());
            giveaway.setGuildId(guildId);
            giveaway.setTitle(title);
            giveaway.setCountWinners(winners);
            giveaway.setEndGiveawayDate(endDate);
            giveaway.setCreatedUserId(createdUserId);
            giveaway.setRoleId(roleId);
            giveaway.setIsForSpecificRole(isForSpecificRole);
            giveaway.setUrlImage(imageUrl);
            giveaway.setFinish(false);
            giveaway.setMinParticipants(minParticipants);

            activeGiveawayRepository.save(giveaway);

            event.reply("Розыгрыш восстановлен.").queue();
        }, throwable -> {
            LOGGER.error("Error while trying to recover giveaway", throwable);
            event.reply("Сообщение не найдено.").setEphemeral(true).queue();
        });
    }
}