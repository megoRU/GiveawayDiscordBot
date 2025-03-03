package main.core.events;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.service.GiveawayRepositoryService;
import main.service.SlashService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class StartCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(StartCommand.class.getName());
    private final GiveawayRepositoryService giveawayRepositoryService;

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void start(@NotNull SlashCommandInteractionEvent event, UpdateController updateController) {
        GuildMessageChannelUnion channel = event.getGuildChannel();
        Guild guild = event.getGuild();
        if (guild == null) return;

        boolean checkPermissions = GiveawayUtils.checkPermissions(channel, guild.getSelfMember());

        if (!checkPermissions) {
            Long check = SlashService.getCommandId("check");
            String botPermissionsDeny = String.format(jsonParsers.getLocale("bot_permissions_deny", guild.getIdLong()), check);
            event.reply(botPermissionsDeny).queue();
            return; //Сообщение уже отправлено
        }

        var guildIdLong = Objects.requireNonNull(guild).getIdLong();
        var guildId = Objects.requireNonNull(guild).getIdLong();
        var userIdLong = event.getUser().getIdLong();
        String title = event.getOption("title", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(1);
        String time = event.getOption("duration", OptionMapping::getAsString);
        if (time != null) time = time.replaceAll("-", ".");
        Long role = event.getOption("mention", OptionMapping::getAsLong);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        int minParticipants = Optional.ofNullable(event.getOption("min-participants", OptionMapping::getAsInt)).orElse(1);
        var urlImage = image != null ? image.getUrl() : null;
        boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

        try {
            if (time != null && !time.matches(GiveawayUtils.TIME_REGEX)) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.RED);

                String startExamples = jsonParsers.getLocale("start_examples", guildId);
                String startWrongTime = String.format(jsonParsers.getLocale("start_wrong_time", guildId), time, startExamples);

                embedBuilder.setDescription(startWrongTime);
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                return;
            }

            if (time != null) {
                if (!GiveawayUtils.isISOTimeCorrect(time) && !GiveawayUtils.isTimeCorrect(time)) {
                    String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.RED);
                    builder.setTitle(wrongDate);
                    builder.setDescription(wrongDate);
                    event.replyEmbeds(builder.build()).queue();
                    return;
                }
            }

            if (role == null && isOnlyForSpecificRole) {
                String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                event.reply(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                return;
            } else if (role != null && role == guildIdLong && isOnlyForSpecificRole) {
                String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                event.reply(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                return;
            } else if (role != null && !isOnlyForSpecificRole) {
                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                if (role == guildIdLong) {
                    giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                    event.reply(giftNotificationForThisRole).queue();
                } else {
                    event.reply(giftNotificationForThisRole).queue();
                }
            } else if (role != null) {
                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                event.reply(giftNotificationForThisRole).queue();
            }

            Giveaway giveaway = new Giveaway(guildIdLong,
                    event.getChannel().getIdLong(),
                    userIdLong,
                    giveawayRepositoryService,
                    updateController);

            GiveawayRegistry instance = GiveawayRegistry.getInstance();

            if (!event.getInteraction().isAcknowledged()) {
                try {
                    String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", guildId), event.getChannel().getId());
                    event.reply(sendSlashMessage).setEphemeral(true).queue();
                } catch (Exception ignored) {
                }
            }

            giveaway.startGiveaway(
                    event.getGuildChannel(),
                    title,
                    winners,
                    time,
                    role,
                    isOnlyForSpecificRole,
                    urlImage,
                    false,
                    minParticipants);

            long messageId = giveaway.getGiveawayData().getMessageId();
            instance.putGift(messageId, giveaway);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            String slashErrors = jsonParsers.getLocale("slash_errors", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(slashErrors);
            if (event.isAcknowledged()) event.getHook().editOriginalEmbeds(errors.build()).queue();
            else event.replyEmbeds(errors.build()).queue();
        }
    }
}