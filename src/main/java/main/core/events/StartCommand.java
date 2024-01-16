package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.*;
import main.giveaway.utils.ChecksClass;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;

@Service
@AllArgsConstructor
public class StartCommand {
    private static final JSONParsers jsonParsers = new JSONParsers();

    private final ListUsersRepository listUsersRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final SchedulingRepository schedulingRepository;
    private final GiveawayMessageHandler giveawayMessageHandler;
    private final GiveawaySaving giveawaySaving;
    private final GiveawayEnd giveawayEnd;

    public void start(@NotNull SlashCommandInteractionEvent event) {
        boolean canSendGiveaway = ChecksClass.canSendGiveaway(event.getGuildChannel(), event);
        if (!canSendGiveaway) return; //Сообщение уже отправлено

        if (event.getGuild() == null) return;

        var guildId = event.getGuild().getIdLong();
        var userIdLong = event.getUser().getIdLong();
        var title = event.getOption("title", OptionMapping::getAsString);
        var countString = event.getOption("count", OptionMapping::getAsString);
        var time = event.getOption("duration", OptionMapping::getAsString);
        if (time != null) time = time.replaceAll("-", ".");
        var role = event.getOption("mention", OptionMapping::getAsRole);
        var isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

        Message.Attachment image = event.getOption("image", OptionMapping::getAsAttachment);
        Integer minParticipants = event.getOption("min_participants", OptionMapping::getAsInt);
        var forbiddenRole = event.getOption("forbidden_role", OptionMapping::getAsRole);

        Scheduling schedulingByGuildLongId = schedulingRepository.findByGuildLongId(guildId);
        if (GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.replyEmbeds(errors.build()).queue();
        } else if (schedulingByGuildLongId != null) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_cancel_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.replyEmbeds(errors.build()).queue();
        } else {
            try {
                String urlImage = null;
                int count = 1;
                if (countString != null) count = Integer.parseInt(countString);

                if (minParticipants == null || minParticipants < 2) {
                    minParticipants = 2;
                }

                if (image != null && image.isImage()) {
                    urlImage = image.getUrl();
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.BLACK);

                if (time != null && !time.matches(GiveawayUtils.TIME_REGEX)) {
                    String startExamples = jsonParsers.getLocale("start_examples", guildId);
                    String startWrongTime = String.format(jsonParsers.getLocale("start_wrong_time", guildId), time, startExamples);

                    embedBuilder.setDescription(startWrongTime);
                    event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                    return;
                }

                if (title != null && title.length() >= MessageEmbed.TITLE_MAX_LENGTH) {
                    String slashError256 = jsonParsers.getLocale("slash_error_256", guildId);
                    embedBuilder.setDescription(slashError256);
                    event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                    return;
                }

                if (time != null && time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
                    if (GiveawayUtils.timeHandler(event, guildId, time)) return;
                }

                if (role == null && isOnlyForSpecificRole) {
                    String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                    event.reply(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                    return;
                } else if (role != null && role.getIdLong() == guildId && isOnlyForSpecificRole) {
                    String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                    event.reply(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                    return;
                } else if (role != null && !isOnlyForSpecificRole) {
                    String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                    if (role.getIdLong() == guildId) {
                        giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                        event.reply(giftNotificationForThisRole).queue();
                    } else {
                        event.reply(giftNotificationForThisRole).queue();
                    }
                } else if (role != null) {
                    String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                    event.reply(giftNotificationForThisRole).queue();
                }

                GiveawayBuilder.Builder giveawayBuilder = new GiveawayBuilder.Builder();
                giveawayBuilder.setGiveawayEnd(giveawayEnd);
                giveawayBuilder.setActiveGiveawayRepository(activeGiveawayRepository);
                giveawayBuilder.setGiveawaySaving(giveawaySaving);
                giveawayBuilder.setParticipantsRepository(participantsRepository);
                giveawayBuilder.setListUsersRepository(listUsersRepository);
                giveawayBuilder.setGiveawayMessageHandler(giveawayMessageHandler);

                giveawayBuilder.setTextChannelId(event.getChannel().getIdLong());
                giveawayBuilder.setUserIdLong(userIdLong);
                giveawayBuilder.setGuildId(guildId);
                giveawayBuilder.setTitle(title);
                giveawayBuilder.setCountWinners(count);
                giveawayBuilder.setTime(time);
                giveawayBuilder.setRoleId(role != null ? role.getIdLong() : null);
                giveawayBuilder.setForSpecificRole(isOnlyForSpecificRole);
                giveawayBuilder.setUrlImage(urlImage);
                giveawayBuilder.setMinParticipants(minParticipants);
                giveawayBuilder.setForbiddenRole(forbiddenRole != null ? forbiddenRole.getIdLong() : null);

                Giveaway giveaway = giveawayBuilder.build();
                GiveawayRegistry.getInstance().putGift(guildId, giveaway);

                if (!event.isAcknowledged()) {
                    try {
                        String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", guildId), event.getChannel().getId());
                        event.reply(sendSlashMessage).setEphemeral(true).queue();
                    } catch (Exception ignored) {
                    }
                }

                giveaway.startGiveaway(event.getChannel().asGuildMessageChannel(), false);

            } catch (Exception e) {
                if (!e.getMessage().contains("Time in the past")) {
                    e.printStackTrace();
                }
                String slashErrors = jsonParsers.getLocale("slash_errors", guildId);
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.GREEN);
                errors.setDescription(slashErrors);
                if (event.isAcknowledged()) event.getHook().editOriginalEmbeds(errors.build()).queue();
                else event.getChannel().sendMessageEmbeds(errors.build()).queue();
                GiveawayRegistry.getInstance().removeGiveaway(guildId);
                activeGiveawayRepository.deleteById(guildId);
            }
        }
    }
}