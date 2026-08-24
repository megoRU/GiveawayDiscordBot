package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.*;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.service.GiveawayRepositoryService;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EditGiveawayCommand {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final UpdateController updateController;

    private static final JSONParsers jsonParsers = new JSONParsers();

    public record GiveawayInfo(String title, int countWinners, int minParticipants, Instant endGiveawayDate, long userIdLong) {}

    @Transactional
    public void editGiveaway(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();
        String time = event.getOption("duration", OptionMapping::getAsString);

        if (time != null) {
            if (!GiveawayUtils.isISOTimeCorrect(time) && !GiveawayUtils.isTimeCorrect(time)) {
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

        GiveawayInfo giveawayData = handleGiveaway(event);
        if (giveawayData == null) return;

        int minParticipants = giveawayData.minParticipants();

        Instant endGiveaway = giveawayData.endGiveawayDate();
        long userIdLong = giveawayData.userIdLong();

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
                    giveawayEditTitle, giveawayData.title(),
                    giveawayEditWinners, giveawayData.countWinners(),
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
                    giveawayEditTitle, giveawayData.title(),
                    giveawayEditWinners, giveawayData.countWinners(),
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

        Set<Long> participantsList = activeGiveaways.getParticipants() != null ? activeGiveaways.getParticipants()
                .stream()
                .map(Participants::getUserId)
                .collect(Collectors.toSet()) : Collections.emptySet();

        Giveaway giveaway = new Giveaway(
                activeGiveaways.getGuildId(),
                activeGiveaways.getChannelId(),
                activeGiveaways.getCreatedUserId(),
                activeGiveaways.isFinish(),
                activeGiveaways.getMessageId(),
                activeGiveaways.getCountWinners(),
                activeGiveaways.getRoleId(),
                activeGiveaways.getIsForSpecificRole(),
                activeGiveaways.getUrlImage(),
                activeGiveaways.getTitle(),
                activeGiveaways.getEndGiveawayDate(),
                activeGiveaways.getMinParticipants() == null ? 1 : activeGiveaways.getMinParticipants(),
                giveawayRepositoryService,
                updateController
        );
        giveaway.setParticipantsList(participantsList);

        if (title != null) {
            giveaway.setTitle(title);
        }

        if (winners != -1) {
            giveaway.setCountWinners(winners);
        }

        if (time != null) {
            giveaway.updateTime(time);
        }

        if (urlImage != null) {
            giveaway.setUrlImage(urlImage);
        }

        if (minParticipants != null) {
            giveaway.setMinParticipants(minParticipants);
        }

        updateGiveaway(giveaway);

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(giveaway);
        updateController.setView(embedBuilder.build(), giveaway.getGuildId(), giveaway.getTextChannelId(), giveaway.getMessageId());

        return new GiveawayInfo(giveaway.getTitle(), giveaway.getCountWinners(), giveaway.getMinParticipants(), giveaway.getEndGiveawayDate(), giveaway.getUserIdLong());
    }

    private void updateGiveaway(Giveaway giveaway) {
        long messageId = giveaway.getMessageId();
        String title = giveaway.getTitle();
        Instant endGiveawayDate = giveaway.getEndGiveawayDate();
        long textChannelId = giveaway.getTextChannelId();
        int countWinners = giveaway.getCountWinners();
        long guildId = giveaway.getGuildId();
        long userIdLong = giveaway.getUserIdLong();
        int minParticipants = giveaway.getMinParticipants();
        Long roleId = giveaway.getRoleId();
        String urlImage = giveaway.getUrlImage();
        boolean forSpecificRole = giveaway.isForSpecificRole();

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setMessageId(messageId);
        activeGiveaways.setTitle(title);
        activeGiveaways.setGuildId(guildId);
        activeGiveaways.setChannelId(textChannelId);
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setFinish(false);
        activeGiveaways.setIsForSpecificRole(forSpecificRole);
        activeGiveaways.setMinParticipants(minParticipants);
        activeGiveaways.setEndGiveawayDate(endGiveawayDate);
        activeGiveaways.setRoleId(roleId);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setCreatedUserId(userIdLong);

        activeGiveawayRepository.save(activeGiveaways);
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