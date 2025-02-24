package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.GiveawayRegistry;
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

import java.awt.*;
import java.util.List;

@Service
@AllArgsConstructor
public class CancelCommand {

    private final SchedulingRepository schedulingRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void cancel(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        event.deferReply().setEphemeral(true).queue();

        String giveawayId = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (giveawayId != null) {
            handleCancel(giveawayId, userId, guildId);

            event.getHook().sendMessage("Успешно удалено").setEphemeral(true).queue();
        } else {
            List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findByGuildId(guildId);

            if (activeGiveawaysList != null && activeGiveawaysList.size() > 1) {
                event.getHook().sendMessage("""
                                У вас более двух активных `Giveaway`. Используйте параметр `giveaway-id`,
                                чтобы определить, какой из них редактировать.
                                """)
                        .setEphemeral(true)
                        .queue();
            } else if (activeGiveawaysList != null && activeGiveawaysList.size() == 1) {
                ActiveGiveaways first = activeGiveawaysList.getFirst();
                String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);

                removeActiveGiveaway(String.valueOf(first.getMessageId()), guildId);

                EmbedBuilder cancel = new EmbedBuilder();
                cancel.setColor(Color.GREEN);
                cancel.setDescription(cancelGiveaway);

                event.getHook().sendMessageEmbeds(cancel.build()).setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("""
                                У вас нет активных Giveaway. Если вам нужно завершить запланированный,
                                используйте параметр `giveaway-id`.
                                """)
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    private void handleCancel(String giveawayId, long userId, long guildId) {
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByCreatedUserIdAndMessageId(userId, Long.valueOf(giveawayId));
        if (activeGiveaways != null) {
            removeActiveGiveaway(giveawayId, guildId);
        } else {
            Scheduling scheduling = schedulingRepository.findByCreatedUserIdAndIdSalt(userId, giveawayId);
            if (scheduling != null) {
                removeScheduling(giveawayId, guildId);
            }
        }
    }

    private void removeScheduling(String giveawayId, long guildId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeScheduling(guildId);
        instance.removeGuildFromGiveaway(guildId);

        schedulingRepository.deleteByIdSalt(giveawayId);
    }

    private void removeActiveGiveaway(String giveawayId, long guildId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeGuildFromGiveaway(guildId);

        activeGiveawayRepository.deleteByMessageId(Long.valueOf(giveawayId));
    }
}