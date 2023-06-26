package main.core.events;

import main.giveaway.ChecksClass;
import main.giveaway.GiveawayRegistry;
import main.giveaway.impl.Formats;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class SchedulingCommand {

    private final SchedulingRepository schedulingRepository;

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public SchedulingCommand(SchedulingRepository schedulingRepository) {
        this.schedulingRepository = schedulingRepository;
    }

    public void scheduling(@NotNull SlashCommandInteractionEvent event) {
        var guildIdLong = Objects.requireNonNull(event.getGuild()).getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getId();
        var userIdLong = event.getUser().getIdLong();
        Long role = event.getOption("mention", OptionMapping::getAsLong);
        String countString = event.getOption("count", OptionMapping::getAsString);
        String title = event.getOption("title", OptionMapping::getAsString);
        GuildChannelUnion textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);
        Message.Attachment image = event.getOption("image", OptionMapping::getAsAttachment);
        String urlImage = image != null ? image.getUrl() : null;
        Integer minParticipants = event.getOption("min_participants", OptionMapping::getAsInt);
        String startTime = event.getOption("start_time", OptionMapping::getAsString);
        String endTime = event.getOption("end_time", OptionMapping::getAsString);

        if (textChannel == null) {
            event.reply("TextChannel is `Null`").queue();
            return;
        }

        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannel, event);
        if (!canSendGiveaway) return; //Сообщение уже отправлено

        Scheduling scheduling = schedulingRepository.findByGuildLongId(guildIdLong);

        if (GiveawayRegistry.getInstance().hasGiveaway(guildIdLong)) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.replyEmbeds(errors.build()).queue();
            return;
        } else if (scheduling != null) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_cancel_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.replyEmbeds(errors.build()).queue();
            return;
        }

        if ((startTime != null && !startTime.matches(Formats.ISO_TIME_REGEX)
                || (endTime != null && !endTime.matches(Formats.ISO_TIME_REGEX)))) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.reply(wrongDate).queue();
            return;
        }

        if (textChannel instanceof NewsChannel || textChannel instanceof TextChannel) {
            int count = 1;
            if (countString != null) count = Integer.parseInt(countString);

            if (minParticipants == null || minParticipants == 0 || minParticipants == 1) {
                minParticipants = 2;
            }

            if (image != null && image.isImage()) {
                urlImage = image.getUrl();
            }

            boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

            if (role == null && isOnlyForSpecificRole) {
                String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                event.reply(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                return;
            } else if (role != null && role == guildIdLong && isOnlyForSpecificRole) {
                String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                event.reply(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                return;
            }

            scheduling = new Scheduling();
            scheduling.setGuildLongId(guildIdLong);
            scheduling.setChannelIdLong(textChannel.getIdLong());
            scheduling.setCountWinners(count);
            scheduling.setDateCreateGiveaway(timeProcessor(startTime));
            scheduling.setDateEndGiveaway(timeProcessor(endTime) == null ? null : timeProcessor(endTime));
            scheduling.setGiveawayTitle(title);
            scheduling.setRoleIdLong(role);
            scheduling.setIsForSpecificRole(isOnlyForSpecificRole);
            scheduling.setIdUserWhoCreateGiveaway(userIdLong);
            scheduling.setUrlImage(urlImage);
            scheduling.setMinParticipants(minParticipants);

            schedulingRepository.save(scheduling);

            String scheduleEnd = jsonParsers.getLocale("schedule_end", guildId);
            long timeStart = Objects.requireNonNull(timeProcessor(startTime)).getTime() / 1000;
            if (endTime != null) {
                long timeEnd = Objects.requireNonNull(timeProcessor(endTime)).getTime() / 1000;
                if (timeEnd != 0) {
                    scheduleEnd = String.format("<t:%s:R> (<t:%s:f>)", timeEnd, timeEnd);
                }
            }

            String scheduleStart = String.format(jsonParsers.getLocale("schedule_start", guildId),
                    timeStart,
                    timeStart,
                    scheduleEnd,
                    textChannel.getId());
            EmbedBuilder start = new EmbedBuilder();
            start.setColor(Color.GREEN);
            start.setDescription(scheduleStart);

            event.replyEmbeds(start.build()).setEphemeral(true).queue();
        } else {
            String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", guildId);
            event.reply(startInNotTextChannels).queue();
        }
    }

    private Timestamp timeProcessor(String time) {
        if (time == null) return null;
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime = LocalDateTime.parse(time, Formats.FORMATTER);
        long toEpochSecond = localDateTime.toEpochSecond(offset);
        return new Timestamp(toEpochSecond * 1000);
    }
}
