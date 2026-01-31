package main.core.events;

import main.config.BotStart;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Objects;
import java.util.Optional;

import static main.giveaway.GiveawayUtils.getEpochSecond;
import static main.giveaway.GiveawayUtils.timeProcessor;

@Service
public class SchedulingCommand {

    private final SchedulingRepository schedulingRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulingCommand.class.getName());

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public SchedulingCommand(SchedulingRepository schedulingRepository) {
        this.schedulingRepository = schedulingRepository;
    }

    public void scheduling(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        var userId = event.getUser().getIdLong();
        var role = event.getOption("mention", OptionMapping::getAsLong);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(1);
        var title = Optional.ofNullable(event.getOption("title", OptionMapping::getAsString)).orElse("Giveaway");
        var textChannel = event.getOption("text-channel", OptionMapping::getAsChannel);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        var urlImage = image != null ? image.getUrl() : null;
        int minParticipants = Optional.ofNullable(event.getOption("min-participants", OptionMapping::getAsInt)).orElse(1);
        var startTime = Objects.requireNonNull(event.getOption("start-time", OptionMapping::getAsString));
        var endTime = event.getOption("end-time", OptionMapping::getAsString);
        boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

        if (textChannel == null && event.getChannel() instanceof GuildChannelUnion) {
            textChannel = (GuildChannelUnion) event.getChannel();
        }

        boolean checkPermissions = GiveawayUtils.checkPermissions(textChannel, event.getGuild().getSelfMember());

        if (!checkPermissions) {
            String botPermissionsDeny = jsonParsers.getLocale("bot_permissions_deny", guildId);
            event.reply(botPermissionsDeny).queue();
            return; //Сообщение уже отправлено
        }

        //Обработать уведомление
        event.deferReply().setEphemeral(true).queue();

        if (!GiveawayUtils.isISOTimeCorrect(startTime)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.getHook().sendMessage(wrongDate).queue();
            return;
        }

        if (endTime != null && !GiveawayUtils.isISOTimeCorrect(endTime)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.getHook().sendMessage(wrongDate).queue();
            return;
        }

        if (textChannel instanceof NewsChannel || textChannel instanceof TextChannel) {
            if (role == null && isOnlyForSpecificRole) {
                String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                event.getHook().sendMessage(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                return;
            } else if (role != null && role == guildId && isOnlyForSpecificRole) {
                String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                event.getHook().sendMessage(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                return;
            }
            String salt = GiveawayUtils.getSalt(20);

            String zonesId = BotStart.getZonesIdByUser(userId);
            ZoneId zoneId = ZoneId.of(zonesId);

            Instant oneMoths = Instant.now().atZone(zoneId).plusDays(30).toInstant();

            Scheduling scheduling = new Scheduling();
            try {

                scheduling.setIdSalt(salt);
                scheduling.setGuildId(guildId);
                scheduling.setChannelId(textChannel.getIdLong());
                scheduling.setCountWinners(winners);
                scheduling.setDateCreateGiveaway(timeProcessor(startTime, userId));
                scheduling.setDateEndGiveaway(timeProcessor(endTime, userId) == null ? oneMoths : timeProcessor(endTime, userId));
                scheduling.setTitle(title);
                scheduling.setRoleId(role);
                scheduling.setIsForSpecificRole(isOnlyForSpecificRole);
                scheduling.setCreatedUserId(userId);
                scheduling.setUrlImage(urlImage);
                scheduling.setMinParticipants(minParticipants);

                schedulingRepository.save(scheduling);

                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.putScheduling(salt, scheduling);
            } catch (ZoneRulesException z) {
                LOGGER.error(z.getMessage(), z);
                String startWithBrokenZone = jsonParsers.getLocale("start_with_broken_zone", guildId);

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.GREEN);
                errors.setDescription(startWithBrokenZone);

                if (event.isAcknowledged()) event.getHook().editOriginalEmbeds(errors.build()).queue();
                else event.replyEmbeds(errors.build()).queue();

                return;
            }

            Instant giveawayCreateTime = scheduling.getDateCreateGiveaway();
            Instant giveawayEndTime = scheduling.getDateEndGiveaway();

            long timeStart = getEpochSecond(giveawayCreateTime, userId);
            long timeEnd = getEpochSecond(giveawayEndTime, userId);

            String scheduleEnd = String.format("<t:%s:R> (<t:%s:f>)", timeEnd, timeEnd);
            String scheduleRole = jsonParsers.getLocale("schedule_end", guildId);

            String scheduleStart = String.format(jsonParsers.getLocale("schedule_start", guildId),
                    timeStart,
                    timeStart,
                    scheduleEnd,
                    role == null ? scheduleRole : "<@&" + role + ">",
                    textChannel.getId(),
                    salt);

            EmbedBuilder start = new EmbedBuilder();
            start.setColor(GiveawayUtils.getUserColor(guildId));
            start.setDescription(scheduleStart);

            event.getHook().sendMessageEmbeds(start.build()).setEphemeral(true).queue();
        } else {
            String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", guildId);
            event.getHook().sendMessage(startInNotTextChannels).queue();
        }
    }
}