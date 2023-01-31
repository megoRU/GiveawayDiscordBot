package main.core.events;

import main.controller.UpdateController;
import main.giveaway.ChecksClass;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.impl.Formats;
import main.jsonparser.JSONParsers;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class StartCommand {

    private final ListUsersRepository listUsersRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;


    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public StartCommand(ListUsersRepository listUsersRepository, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.listUsersRepository = listUsersRepository;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    public void start(@NotNull SlashCommandInteractionEvent event, UpdateController updateController) {
        var guildIdLong = Objects.requireNonNull(event.getGuild()).getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getId();
        var userIdLong = event.getUser().getIdLong();

        GuildMessageChannel textChannelEvent = null;
        if (GiveawayRegistry.getInstance().hasGiveaway(guildIdLong)) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", guildId);

            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);

            event.replyEmbeds(errors.build()).queue();
        } else {
            GuildChannelUnion textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);

            //Если не указали конкретный канал
            if (textChannel == null) {
                if (event.getChannel() instanceof TextChannel) {
                    textChannelEvent = event.getChannel().asTextChannel();
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                    if (!canSendGiveaway) return;
                } else if (event.getChannel() instanceof NewsChannel) {
                    textChannelEvent = event.getChannel().asNewsChannel();
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                    if (!canSendGiveaway) return;
                }
                //Если указывали
            } else {
                if (textChannel instanceof TextChannel) {
                    textChannelEvent = textChannel.asTextChannel();
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                    if (!canSendGiveaway) return;
                } else if (textChannel instanceof NewsChannel) {
                    textChannelEvent = textChannel.asNewsChannel();
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                    if (!canSendGiveaway) return;
                } else {
                    String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", guildId);
                    event.reply(startInNotTextChannels).queue();
                    return;
                }
            }
            if (textChannelEvent == null) {
                event.reply("TextChannel is `Null`").queue();
                return;
            }

            try {
                String title = event.getOption("title", OptionMapping::getAsString);

                int count = 1;
                String countString = event.getOption("count", OptionMapping::getAsString);
                if (countString != null) count = Integer.parseInt(countString);
                String time = event.getOption("duration", OptionMapping::getAsString);
                if (time != null) time = time.replaceAll("-", ".");
                Long role = event.getOption("mention", OptionMapping::getAsLong);
                Message.Attachment image = event.getOption("image", OptionMapping::getAsAttachment);
                String urlImage = null;

                if (image != null && image.isImage()) {
                    urlImage = image.getUrl();
                }

                boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.BLACK);

                if (time != null && !time.matches(Formats.TIME_REGEX)) {
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

                if (time != null && time.matches(Formats.ISO_TIME_REGEX)) {
                    if (timeHandler(event, guildId, time)) return;
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
                        textChannelEvent.getIdLong(),
                        userIdLong,
                        activeGiveawayRepository,
                        participantsRepository,
                        listUsersRepository,
                        updateController);

                GiveawayRegistry.getInstance().putGift(guildIdLong, giveaway);

                String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", guildId), textChannelEvent.getId());

                if (!event.isAcknowledged()) {
                    try {
                        event.reply(sendSlashMessage)
                                .delay(5, TimeUnit.SECONDS)
                                .flatMap(InteractionHook::deleteOriginal)
                                .queue();
                    } catch (Exception ignored) {
                    }
                }

                giveaway.startGiveaway(
                        textChannelEvent,
                        title,
                        count,
                        time,
                        role,
                        isOnlyForSpecificRole,
                        urlImage,
                        false);

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
                GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildIdLong);
                activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
            }
        }
    }

    private boolean timeHandler(@NotNull SlashCommandInteractionEvent event, String guildId, String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, Formats.FORMATTER);
        LocalDateTime now = Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime();
        if (localDateTime.isBefore(now)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", (guildId));
            String youWroteDate = jsonParsers.getLocale("you_wrote_date", (guildId));

            String format = String.format(youWroteDate,
                    localDateTime.toString().replace("T", " "),
                    now.toString().substring(0, 16).replace("T", " "));

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(Color.RED);
            builder.setTitle(wrongDate);
            builder.setDescription(format);

            event.replyEmbeds(builder.build()).queue();
            return true;
        }
        return false;
    }
}
