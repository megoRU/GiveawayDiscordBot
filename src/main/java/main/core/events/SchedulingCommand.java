package main.core.events;

import main.giveaway.utils.ChecksClass;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;
import java.util.Optional;

@Service
public class SchedulingCommand {

    private final SchedulingRepository schedulingRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public SchedulingCommand(SchedulingRepository schedulingRepository,
                             ActiveGiveawayRepository activeGiveawayRepository) {
        this.schedulingRepository = schedulingRepository;
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void scheduling(@NotNull SlashCommandInteractionEvent event) {
        var guildIdLong = Objects.requireNonNull(event.getGuild()).getIdLong();
        var guildId = event.getGuild().getIdLong();
        var userIdLong = event.getUser().getIdLong();
        var role = event.getOption("mention", OptionMapping::getAsLong);
        var countString = event.getOption("count", OptionMapping::getAsString);
        var title = event.getOption("title", OptionMapping::getAsString);
        var textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        var urlImage = image != null ? image.getUrl() : null;
        var minParticipants = event.getOption("min_participants", OptionMapping::getAsInt);
        var startTime = event.getOption("start_time", OptionMapping::getAsString);
        var endTime = event.getOption("end_time", OptionMapping::getAsString);
        var forbiddenRole = event.getOption("forbidden_role", OptionMapping::getAsRole);
        boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

        if (textChannel == null) {
            event.reply("TextChannel is `Null`").queue();
            return;
        }

        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannel, event);
        if (!canSendGiveaway) return; //Сообщение уже отправлено

        //Обработать уведомление
        event.deferReply().setEphemeral(true).queue();

        Scheduling scheduling = schedulingRepository.findByGuildLongId(guildIdLong);
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByGuildLongId(guildIdLong);

        if (activeGiveaways != null) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.getHook().sendMessageEmbeds(errors.build()).queue();
            return;
        } else if (scheduling != null) {
            String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_cancel_giveaway", guildId);
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setDescription(messageGiftNeedStopGiveaway);
            event.getHook().sendMessageEmbeds(errors.build()).queue();
            return;
        }

        if ((startTime != null && !startTime.matches(GiveawayUtils.ISO_TIME_REGEX)
                || (endTime != null && !endTime.matches(GiveawayUtils.ISO_TIME_REGEX)))) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.getHook().sendMessage(wrongDate).queue();
            return;
        }

        if (textChannel instanceof NewsChannel || textChannel instanceof TextChannel) {
            int count = 1;
            if (countString != null) count = Integer.parseInt(countString);

            if (minParticipants == null || minParticipants < 2) {
                minParticipants = 2;
            }

            if (image != null && image.isImage()) {
                urlImage = image.getUrl();
            }

            if (role == null && isOnlyForSpecificRole) {
                String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                event.getHook().sendMessage(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                return;
            } else if (role != null && role == guildIdLong && isOnlyForSpecificRole) {
                String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                event.getHook().sendMessage(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                return;
            }

            scheduling = new Scheduling();
            scheduling.setGuildLongId(guildIdLong);
            scheduling.setChannelIdLong(textChannel.getIdLong());
            scheduling.setCountWinners(count);
            scheduling.setDateCreateGiveaway(GiveawayUtils.timeProcessor(startTime));
            scheduling.setDateEndGiveaway(GiveawayUtils.timeProcessor(endTime) == null ? null : GiveawayUtils.timeProcessor(endTime));
            scheduling.setGiveawayTitle(title);
            scheduling.setRoleIdLong(role);
            scheduling.setIsForSpecificRole(isOnlyForSpecificRole);
            scheduling.setIdUserWhoCreateGiveaway(userIdLong);
            scheduling.setUrlImage(urlImage);
            scheduling.setMinParticipants(minParticipants);
            scheduling.setForbiddenRole(forbiddenRole != null ? forbiddenRole.getIdLong() : null);

            schedulingRepository.save(scheduling);

            String scheduleEnd = jsonParsers.getLocale("schedule_end", guildId);
            long timeStart = Objects.requireNonNull(GiveawayUtils.timeProcessor(startTime)).getTime() / 1000;
            if (endTime != null) {
                long timeEnd = Objects.requireNonNull(GiveawayUtils.timeProcessor(endTime)).getTime() / 1000;
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

            event.getHook().sendMessageEmbeds(start.build()).setEphemeral(true).queue();
        } else {
            String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", guildId);
            event.getHook().sendMessage(startInNotTextChannels).queue();
        }
    }
}