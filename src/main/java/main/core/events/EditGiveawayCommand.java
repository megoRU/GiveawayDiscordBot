package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.GiveawayEmbedUtils;
import main.giveaway.GiveawayInfo;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EditGiveawayCommand {
    private static final JSONParsers jsonParsers = new JSONParsers();

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final UpdateController updateController;

    @Transactional
    public void editGiveaway(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();
        String time = event.getOption("duration", OptionMapping::getAsString);

        if (time != null) {
            if (!GiveawayUtils.isTimeISOStandard(time) && !GiveawayUtils.isTimeCorrect(time)) {
                String changeDuration = jsonParsers.getLocale("wrong_date", guildId);
                event.getHook().sendMessage(changeDuration).setEphemeral(true).queue();
                return;
            }
        }

        String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
        String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
        String giveawayEdit = jsonParsers.getLocale("giveaway_edit", guildId);
        String giveawayEditEnds = jsonParsers.getLocale("giveaway_edit_ends", guildId);
        String listMenuParticipants = jsonParsers.getLocale("list_menu_participants", guildId);

        GiveawayInfo giveawayInfo = handleGiveaway(event);
        if (giveawayInfo == null) return;

        int minParticipants = giveawayInfo.minParticipants();

        Instant endGiveaway = giveawayInfo.endGiveawayDate();
        long userIdLong = giveawayInfo.userIdLong();

        String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
        ZoneId userOffset = ZoneId.of(zonesIdByUser);

        LocalDateTime userTime = endGiveaway.atZone(userOffset).toLocalDateTime();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(GiveawayUtils.getUserColor(guildId));

        if (userTime == null) {
            embedBuilder.setFooter(giveawayEdit);
            embedBuilder.setDescription(String.format("""
                            %s `%s`
                            %s `%s`
                            %s `%s`
                            """,
                    giveawayEditTitle, giveawayInfo.title(),
                    giveawayEditWinners, giveawayInfo.countWinners(),
                    listMenuParticipants, minParticipants
            ));

        } else {
            long endTime = userTime.atZone(userOffset).toEpochSecond();
            embedBuilder.setFooter(giveawayEdit);
            embedBuilder.setDescription(String.format("""
                            
                            %s `%s`
                            %s `%s`
                            %s `%s`
                            %s <t:%s:R> (<t:%s:f>)
                            """,
                    giveawayEditTitle, giveawayInfo.title(),
                    giveawayEditWinners, giveawayInfo.countWinners(),
                    listMenuParticipants, minParticipants,
                    giveawayEditEnds, endTime, endTime));
        }

        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private GiveawayInfo handleGiveaway(@NotNull SlashCommandInteractionEvent event) {
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (giveawayId == null) {
            return handleGiveawayByGuild(event, guildId);
        }

        try {
            long giveawayIdLong = Long.parseLong(giveawayId);
            ActiveGiveaways activeGiveaway = activeGiveawayRepository.findByMessageId(giveawayIdLong);
            if (activeGiveaway != null && activeGiveaway.getGuildId().equals(guildId)) {
                return updateActiveGiveaway(event, activeGiveaway);
            } else {
                String selectMenuGiveawayNotFound = jsonParsers.getLocale("select_menu_giveaway_not_found", guildId);
                event.getHook().sendMessage(selectMenuGiveawayNotFound).setEphemeral(true).queue();
            }
        } catch (NumberFormatException ignored) {
            Scheduling scheduling = schedulingRepository.findByIdSalt(giveawayId);

            if (scheduling != null && scheduling.getGuildId().equals(guildId)) {
                return updateSchedulingGiveaway(event, scheduling);
            } else {
                String selectMenuSchedulingNotFound = jsonParsers.getLocale("select_menu_scheduling_not_found", guildId);
                event.getHook().sendMessage(selectMenuSchedulingNotFound).setEphemeral(true).queue();
            }
        }
        return null;
    }

    private GiveawayInfo handleGiveawayByGuild(@NotNull SlashCommandInteractionEvent event, long guildId) {
        List<ActiveGiveaways> giveawayList = activeGiveawayRepository.findByGuildId(guildId);
        if (giveawayList == null) giveawayList = Collections.emptyList();
        List<Scheduling> schedulingList = schedulingRepository.findByGuildId(guildId);

        if (giveawayList.size() == 1 && schedulingList.isEmpty()) {
            return updateActiveGiveaway(event, giveawayList.getFirst());
        } else if (schedulingList.size() == 1 && giveawayList.isEmpty()) {
            return updateSchedulingGiveaway(event, schedulingList.getFirst());
        } else {
            String giveawayEditCommand = jsonParsers.getLocale("giveaway_edit_command", guildId);
            event.getHook().sendMessage(giveawayEditCommand).setEphemeral(true).queue();
            return null;
        }
    }

    private GiveawayInfo updateActiveGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull ActiveGiveaways activeGiveaways) {
        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        var urlImage = image != null ? image.getUrl() : null;
        Integer minParticipants = event.getOption("min-participants", OptionMapping::getAsInt);


        if (title != null) {
            activeGiveaways.setTitle(title);
        }

        if (winners != -1) {
            activeGiveaways.setCountWinners(winners);
        }

        if (time != null) {
            Long createdUserId = activeGiveaways.getCreatedUserId();
            Instant instant = GiveawayUtils.updateTime(time, createdUserId);

            activeGiveaways.setEndGiveawayDate(instant);
        }

        if (urlImage != null) {
            activeGiveaways.setUrlImage(urlImage);
        }

        if (minParticipants != null) {
            activeGiveaways.setMinParticipants(minParticipants);
        }

        activeGiveawayRepository.save(activeGiveaways);

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(activeGiveaways);

        updateController.setView(embedBuilder.build(),
                activeGiveaways.getGuildId(),
                activeGiveaways.getChannelId(),
                activeGiveaways.getMessageId());

        return new GiveawayInfo(
                activeGiveaways.getTitle(),
                activeGiveaways.getCountWinners(),
                activeGiveaways.getMinParticipants(),
                activeGiveaways.getEndGiveawayDate(),
                activeGiveaways.getCreatedUserId());
    }

    private GiveawayInfo updateSchedulingGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Scheduling scheduling) {
        long userId = event.getUser().getIdLong();
        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        String urlImage = image != null ? image.getUrl() : null;
        Integer minParticipants = event.getOption("min-participants", OptionMapping::getAsInt);

        if (title != null) {
            scheduling.setTitle(title);
        }

        if (winners != -1) {
            scheduling.setCountWinners(winners);
        }

        if (time != null) {
            LocalDateTime localDateTime;

            String zonesIdByUser = BotStart.getZonesIdByUser(userId);
            ZoneId zoneId = ZoneId.of(zonesIdByUser);

            if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
                localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
            } else {
                long seconds = GiveawayUtils.getSeconds(time);
                localDateTime = LocalDateTime.now(zoneId).plusSeconds(seconds);
            }

            // Привязываем локальное время пользователя к его зоне
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);

            // Переводим в Instant (UTC)
            Instant utcInstant = zonedDateTime.toInstant();

            scheduling.setDateEndGiveaway(utcInstant);
        }

        if (urlImage != null) {
            scheduling.setUrlImage(urlImage);
        }

        if (minParticipants != null) {
            scheduling.setMinParticipants(minParticipants);
        }

        schedulingRepository.save(scheduling);

        return new GiveawayInfo(scheduling.getTitle(), scheduling.getCountWinners(), scheduling.getMinParticipants() == null ? 1 : scheduling.getMinParticipants(), scheduling.getDateEndGiveaway(), scheduling.getCreatedUserId());
    }

}