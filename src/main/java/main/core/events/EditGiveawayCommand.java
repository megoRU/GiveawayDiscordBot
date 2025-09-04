package main.core.events;

import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.giveaway.*;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EditGiveawayCommand {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final UpdateController updateController;

    private static final JSONParsers jsonParsers = new JSONParsers();

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

        GiveawayData giveawayData = handleGiveaway(event);
        if (giveawayData == null) return;

        int minParticipants = giveawayData.getMinParticipants();

        Instant endGiveaway = giveawayData.getEndGiveawayDate();
        long userIdLong = giveawayData.getUserIdLong();

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
                    giveawayEditTitle, giveawayData.getTitle(),
                    giveawayEditWinners, giveawayData.getCountWinners(),
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
                    giveawayEditTitle, giveawayData.getTitle(),
                    giveawayEditWinners, giveawayData.getCountWinners(),
                    listMenuParticipants, minParticipants,
                    giveawayEditEnds, endTime, endTime));
        }

        event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private GiveawayData handleGiveaway(@NotNull SlashCommandInteractionEvent event) {
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        if (giveawayId == null) {
            return handleGiveawayByGuild(event, guildId, instance);
        }

        try {
            long giveawayIdLong = Long.parseLong(giveawayId);
            Giveaway giveaway = instance.getGiveaway(giveawayIdLong);
            if (giveaway != null) {
                return updateActiveGiveaway(event, giveaway);
            } else {
                String selectMenuGiveawayNotFound = jsonParsers.getLocale("select_menu_giveaway_not_found", guildId);
                event.getHook().sendMessage(selectMenuGiveawayNotFound).setEphemeral(true).queue();
            }
        } catch (NumberFormatException ignored) {
            Scheduling scheduling = instance.getScheduling(giveawayId);
            if (scheduling != null) {
                return updateSchedulingGiveaway(event, scheduling);
            } else {
                String selectMenuSchedulingNotFound = jsonParsers.getLocale("select_menu_scheduling_not_found", guildId);
                event.getHook().sendMessage(selectMenuSchedulingNotFound).setEphemeral(true).queue();
            }
        }
        return null;
    }

    private GiveawayData handleGiveawayByGuild(@NotNull SlashCommandInteractionEvent event, long guildId, GiveawayRegistry instance) {
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);
        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);

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

    private GiveawayData updateActiveGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Giveaway giveaway) {
        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        var urlImage = image != null ? image.getUrl() : null;
        Integer minParticipants = event.getOption("min-participants", OptionMapping::getAsInt);

        long messageId = giveaway.getGiveawayData().getMessageId();
        long guildId = giveaway.getGuildId();
        long channelId = giveaway.getTextChannelId();

        GiveawayData giveawayData = giveaway.getGiveawayData();

        if (title != null) {
            updateTitle(giveaway, title);
        }

        if (winners != -1) {
            updateWinners(giveaway, winners);
        }

        if (time != null) {
            updateTime(giveaway, time);
        }

        if (urlImage != null) {
            updateImage(giveaway, urlImage);
        }

        if (minParticipants != null) {
            updateMinParticipants(giveaway, minParticipants);
        }

        updateGiveaway(giveaway);

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(giveawayData, giveaway);
        updateController.setView(embedBuilder.build(), guildId, channelId, messageId);

        return giveaway.getGiveawayData();
    }

    private void updateGiveaway(Giveaway giveaway) {
        long messageId = giveaway.getGiveawayData().getMessageId();
        String title = giveaway.getGiveawayData().getTitle();
        Instant endGiveawayDate = giveaway.getGiveawayData().getEndGiveawayDate();
        long textChannelId = giveaway.getTextChannelId();
        int countWinners = giveaway.getGiveawayData().getCountWinners();
        long guildId = giveaway.getGuildId();
        long userIdLong = giveaway.getUserIdLong();
        int minParticipants = giveaway.getGiveawayData().getMinParticipants();
        Long roleId = giveaway.getGiveawayData().getRoleId();
        String urlImage = giveaway.getGiveawayData().getUrlImage();
        boolean forSpecificRole = giveaway.getGiveawayData().isForSpecificRole();

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

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.putGift(messageId, giveaway);
    }

    private GiveawayData updateSchedulingGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Scheduling scheduling) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);
        String idSalt = scheduling.getIdSalt();
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
            scheduling.setDateEndGiveaway(Instant.parse(time));
        }

        if (urlImage != null) {
            scheduling.setUrlImage(urlImage);
        }

        if (minParticipants != null) {
            scheduling.setMinParticipants(minParticipants);
        }

        instance.putScheduling(idSalt, scheduling);
        schedulingRepository.save(scheduling);

        return GiveawayData.builder()
                .title(scheduling.getTitle())
                .countWinners(scheduling.getCountWinners())
                .endGiveawayDate(scheduling.getDateEndGiveaway())
                .build();
    }

    private void updateTitle(Giveaway giveaway, String title) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setTitle(title);
    }

    private void updateWinners(Giveaway giveaway, int winners) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setCountWinners(winners);
    }

    private void updateTime(Giveaway giveaway, String time) {
        giveaway.updateTime(time);
    }

    private void updateMinParticipants(@NotNull Giveaway giveaway, Integer minParticipants) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setMinParticipants(minParticipants);
    }

    private void updateImage(@NotNull Giveaway giveaway, String urlImage) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setUrlImage(urlImage);
    }
}