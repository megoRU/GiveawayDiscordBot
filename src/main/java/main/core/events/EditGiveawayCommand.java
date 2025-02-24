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

        try {
            String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
            String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
            String giveawayEdit = jsonParsers.getLocale("giveaway_edit", guildId);
            String giveawayEditEnds = jsonParsers.getLocale("giveaway_edit_ends", guildId);

            GiveawayData giveawayData = handleGiveaway(event);
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
        } catch (IllegalArgumentException e) {
            String giveawayNotFound = jsonParsers.getLocale("giveaway_not_found", guildId);
            event.getHook().sendMessage(giveawayNotFound).queue();
        }
    }

    private GiveawayData handleGiveaway(@NotNull SlashCommandInteractionEvent event) throws IllegalArgumentException {
        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (giveawayId != null) {
            ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByMessageId(Long.parseLong(giveawayId));

            if (activeGiveaways != null) {
                return updateActiveGiveaway(event, activeGiveaways);
            } else {
                Scheduling scheduling = schedulingRepository.findByIdSalt(giveawayId);
                if (scheduling != null) {
                    return updateSchedulingGiveaway(event, scheduling);
                }
            }
        } else {
            List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll()
                    .stream()
                    .filter(activeGiveaways -> activeGiveaways.getGuildId() == guildId)
                    .toList();

            if (activeGiveawaysList.size() > 1) {
                event.getHook().sendMessage("""
                        У вас более двух активных `Giveaway`. Используйте параметр `giveaway-id`,
                        чтобы определить, какой из них редактировать.
                        """).setEphemeral(true).queue();
            } else {
                return updateActiveGiveaway(event, activeGiveawaysList.getFirst());
            }

            List<Scheduling> schedulingList = schedulingRepository.findAll()
                    .stream()
                    .filter(scheduling -> scheduling.getGuildId() == guildId)
                    .toList();

            if (schedulingList.size() > 1) {
                event.getHook().sendMessage("""
                        У вас более двух запланированных `Giveaway`. Используйте параметр `giveaway-id`,
                        чтобы определить, какой из них редактировать.
                        """).setEphemeral(true).queue();
            } else {
                return updateSchedulingGiveaway(event, schedulingList.getFirst());
            }
        }

        throw new IllegalArgumentException("Don't know how to handle giveaway: " + giveawayId);
    }

    private GiveawayData updateActiveGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull ActiveGiveaways activeGiveaway) throws IllegalArgumentException {
        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);

        Long messageId = activeGiveaway.getMessageId();
        Long guildId = activeGiveaway.getGuildId();
        Long channelId = activeGiveaway.getChannelId();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(guildId);
        if (giveaway == null) throw new IllegalArgumentException("Giveaway not found");

        if (title != null) {
            updateTitle(giveaway, activeGiveaway, title);
        }

        if (winners != -1) {
            updateWinners(giveaway, activeGiveaway, winners);
        }

        if (time != null) {
            updateTime(giveaway, activeGiveaway, time);
        }

        activeGiveawayRepository.save(activeGiveaway);

        EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayPattern(guildId);
        updateController.setView(embedBuilder.build(), guildId, channelId, messageId);

        return giveaway.getGiveawayData();
    }

    private GiveawayData updateSchedulingGiveaway(@NotNull SlashCommandInteractionEvent event, @NotNull Scheduling scheduling) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        String time = event.getOption("duration", OptionMapping::getAsString);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(-1);
        String title = event.getOption("title", OptionMapping::getAsString);

        Long guildId = scheduling.getGuildId();


        if (title != null) {
            scheduling.setTitle(title);
        }

        if (winners != -1) {
            scheduling.setCountWinners(winners);
        }

        if (time != null) {
            scheduling.setDateEnd(Timestamp.valueOf(time));
        }

        instance.putScheduling(guildId, scheduling);
        schedulingRepository.save(scheduling);

        return GiveawayData.builder()
                .title(scheduling.getTitle())
                .countWinners(scheduling.getCountWinners())
                .endGiveawayDate(scheduling.getDateEnd())
                .build();
    }

    private void updateTitle(Giveaway giveaway, ActiveGiveaways activeGiveaways, String title) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        giveawayData.setTitle(title);
        activeGiveaways.setTitle(title);
    }

    private void updateWinners(Giveaway giveaway, ActiveGiveaways activeGiveaways, int winners) {
        GiveawayData giveawayData = giveaway.getGiveawayData();

        if (winners != -1) {
            giveawayData.setCountWinners(winners);
            activeGiveaways.setCountWinners(winners);
        }
    }

    private void updateTime(Giveaway giveaway, ActiveGiveaways activeGiveaways, String time) {
        Timestamp timestamp = giveaway.updateTime(time);
        activeGiveaways.setDateEnd(timestamp);
    }
}