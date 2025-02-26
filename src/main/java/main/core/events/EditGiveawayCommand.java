package main.core.events;

import lombok.AllArgsConstructor;
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

import java.sql.Timestamp;
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

        GiveawayData giveawayData = handleGiveaway(event);
        if (giveawayData == null) return;
        Timestamp endGiveawayDate = giveawayData.getEndGiveawayDate();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(GiveawayUtils.getUserColor(guildId));

        if (endGiveawayDate == null) {
            embedBuilder.setFooter(giveawayEdit);
            embedBuilder.setDescription(String.format("""
                            %s `%s`
                            %s `%s`
                            """,
                    giveawayEditTitle, giveawayData.getTitle(),
                    giveawayEditWinners, giveawayData.getCountWinners()));

        } else {
            long endTime = endGiveawayDate.getTime() / 1000;
            embedBuilder.setFooter(giveawayEdit);
            embedBuilder.setDescription(String.format("""
                            
                            %s `%s`
                            %s `%s`
                            %s <t:%s:R> (<t:%s:f>)
                            """,
                    giveawayEditTitle, giveawayData.getTitle(),
                    giveawayEditWinners, giveawayData.getCountWinners(),
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
        if (giveawayList.size() == 1) {
            return updateActiveGiveaway(event, giveawayList.getFirst());
        }

        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);
        if (schedulingList.size() == 1) {
            return updateSchedulingGiveaway(event, schedulingList.getFirst());
        }

        String responseKey = giveawayList.isEmpty() && schedulingList.isEmpty()
                ? "giveaway_not_found"
                : "giveaway_edit_command";

        event.getHook().sendMessage(jsonParsers.getLocale(responseKey, guildId)).setEphemeral(true).queue();
        return null;
    }

    private GiveawayData updateActiveGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Giveaway giveaway) {
        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);

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

        updateGiveaway(giveaway);

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(giveawayData, giveaway);
        updateController.setView(embedBuilder.build(), guildId, channelId, messageId);

        return giveaway.getGiveawayData();
    }

    private void updateGiveaway(Giveaway giveaway) {
        long messageId = giveaway.getGiveawayData().getMessageId();
        String title = giveaway.getGiveawayData().getTitle();
        Timestamp endGiveawayDate = giveaway.getGiveawayData().getEndGiveawayDate();
        long textChannelId = giveaway.getTextChannelId();
        int countWinners = giveaway.getGiveawayData().getCountWinners();
        long guildId = giveaway.getGuildId();
        long userIdLong = giveaway.getUserIdLong();

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setMessageId(messageId);
        activeGiveaways.setFinish(false);
        activeGiveaways.setCreatedUserId(userIdLong);
        activeGiveaways.setGuildId(guildId);
        activeGiveaways.setTitle(title);
        activeGiveaways.setChannelId(textChannelId);
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setDateEnd(endGiveawayDate);

        activeGiveawayRepository.save(activeGiveaways);
    }

    private GiveawayData updateSchedulingGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Scheduling scheduling) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);
        String idSalt = scheduling.getIdSalt();

        if (title != null) {
            scheduling.setTitle(title);
        }

        if (winners != -1) {
            scheduling.setCountWinners(winners);
        }

        if (time != null) {
            scheduling.setDateEnd(Timestamp.valueOf(time));
        }

        instance.putScheduling(idSalt, scheduling);
        schedulingRepository.save(scheduling);

        return GiveawayData.builder()
                .title(scheduling.getTitle())
                .countWinners(scheduling.getCountWinners())
                .endGiveawayDate(scheduling.getDateEnd())
                .build();
    }

    private void updateTitle(Giveaway giveaway, String title) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setTitle(title);
    }

    private void updateWinners(Giveaway giveaway, int winners) {
        GiveawayData giveawayData = giveaway.getGiveawayData();

        if (winners != -1) {
            giveawayData.setCountWinners(winners);
        }
    }

    private void updateTime(Giveaway giveaway, String time) {
        giveaway.updateTime(time);
    }
}