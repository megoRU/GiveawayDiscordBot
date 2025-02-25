package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SelectMenuInteraction {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    public void handle(@NotNull StringSelectInteractionEvent event) {
        if (event.getSelectedOptions().isEmpty()) {
            event.reply("Вы ничего не выбрали.").queue();
            return;
        }

        String selectedValue = event.getSelectedOptions().getFirst().getValue();
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        GiveawayRegistry instance = GiveawayRegistry.getInstance();

        if (selectedValue.startsWith("giveaway_")) {
            handleGiveawaySelection(event, selectedValue, guildId, instance);
        } else if (selectedValue.startsWith("scheduling_")) {
            handleSchedulingSelection(event, selectedValue, guildId, instance);
        } else if (selectedValue.startsWith("stop_")) {
            handleStopGiveaway(event, selectedValue, guildId, instance);
        } else if (selectedValue.startsWith("back_")) {
            handleBackSelection(event, guildId, instance);
        } else if (selectedValue.startsWith("cancel_")) {
            handleCancelSelection(event, selectedValue, guildId, instance);
        } else {
            event.reply("Неизвестный выбор.").queue();
        }
    }

    private void handleGiveawaySelection(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replace("giveaway_", "");
        Giveaway giveaway = instance.getGiveaway(Long.parseLong(messageId));

        if (giveaway != null) {
            String message = formatGiveawayMessage(giveaway, guildId);
            var menu = createGiveawayMenu(giveaway);
            event.editMessage(message).setComponents(ActionRow.of(menu)).queue();
        } else {
            event.reply("Giveaway с таким ID не найден!").queue();
        }
    }

    private void handleSchedulingSelection(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replace("scheduling_", "");
        Scheduling scheduling = instance.getScheduling(messageId);

        if (scheduling != null) {
            String message = formatSchedulingMessage(scheduling, guildId);
            var menu = createSchedulingMenu(scheduling);
            event.editMessage(message).setComponents(ActionRow.of(menu)).queue();
        } else {
            event.reply("Scheduling с таким ID не найден!").queue();
        }
    }

    private void handleStopGiveaway(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replace("stop_", "");
        Giveaway giveaway = instance.getGiveaway(Long.parseLong(messageId));

        if (giveaway != null) {
            giveaway.stopGiveaway(giveaway.getGiveawayData().getCountWinners());
            event.editMessage(jsonParsers.getLocale("slash_stop", guildId)).queue();
        } else {
            event.editMessage(jsonParsers.getLocale("giveaway_not_found_by_id", guildId)).queue();
        }
    }

    private void handleBackSelection(StringSelectInteractionEvent event, long guildId, GiveawayRegistry instance) {
        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);

        // Формируем сообщение
        String message = "**🎉 Активные Giveaway:**\n";
        message += giveawayList.isEmpty() ? "Нет активных Giveaway.\n" : giveawayList.stream()
                .map(g -> "- " + g.getGiveawayData().getTitle() + " | `" + g.getGiveawayData().getMessageId() + "`")
                .collect(Collectors.joining("\n")) + "\n";

        message += "\n**📅 Запланированные Giveaway:**\n";
        message += schedulingList.isEmpty() ? "Нет запланированных Giveaway.\n" : schedulingList.stream()
                .map(s -> "- " + s.getTitle() + " | `" + s.getIdSalt() + "`")
                .collect(Collectors.joining("\n"));

        var menuBuilder = StringSelectMenu.create("select_action");

        giveawayList.forEach(g ->
                menuBuilder.addOption(
                        g.getGiveawayData().getTitle(),
                        "giveaway_" + g.getGiveawayData().getMessageId(),
                        "Просмотр #" + g.getGiveawayData().getMessageId()

                )
        );

        schedulingList.forEach(s ->
                menuBuilder.addOption(
                        s.getTitle(),
                        "scheduling_" + s.getIdSalt(),
                        "Просмотр #" + s.getIdSalt()
                ));

        if (menuBuilder.getOptions().isEmpty()) {
            event.editMessage(message).queue();
        } else {
            var menu = menuBuilder.build();
            var actionRow = ActionRow.of(menu);
            event.editMessage(message).setComponents(actionRow).queue();
        }
    }

    private void handleCancelSelection(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replaceAll("cancel_", "");

        if (messageId.matches("[0+9]+")) {
            long messageIdLong = Long.parseLong(messageId);
            Giveaway giveaway = instance.getGiveaway(messageIdLong);

            if (giveaway == null) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeActiveGiveaway(messageIdLong);

                String cancelGiveaway = jsonParsers.getLocale("cancel_giveaway", guildId);
                event.editMessage(cancelGiveaway).setComponents().queue();
            }
        } else {
            Scheduling scheduling = instance.getScheduling(messageId);

            if (scheduling == null) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeScheduling(messageId);

                String cancelSchedulingGiveaway = jsonParsers.getLocale("cancel_scheduling_giveaway", guildId);
                event.editMessage(cancelSchedulingGiveaway).setComponents().queue();
            }
        }
    }

    private String formatGiveawayMessage(Giveaway giveaway, long guildId) {
        return "**🎉 Giveaway:**\n" +
                "Название: " + giveaway.getGiveawayData().getTitle() + "\n" +
                getDateTranslation(giveaway.getGiveawayData().getEndGiveawayDate(), guildId);
    }

    private String formatSchedulingMessage(Scheduling scheduling, long guildId) {
        return "**🎉 Scheduling:**\n" +
                "Название: " + scheduling.getTitle() + "\n" +
                getDateTranslation(scheduling.getDateEnd(), guildId);
    }

    private StringSelectMenu createGiveawayMenu(Giveaway giveaway) {
        return StringSelectMenu.create("select_action")
                .addOption("Разыграть", "stop_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("🎉"))
                .addOption("Отменить", "cancel_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("❌"))
                .addOption("Назад", "back_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("⬅️"))
                .build();
    }

    private StringSelectMenu createSchedulingMenu(Scheduling scheduling) {
        return StringSelectMenu.create("select_action")
                .addOption("Отменить", "cancel_" + scheduling.getIdSalt(), Emoji.fromUnicode("❌"))
                .addOption("Назад", "back_" + scheduling.getIdSalt(), Emoji.fromUnicode("⬅️"))
                .build();
    }

    private String getDateTranslation(Timestamp timestamp, long guildId) {
        if (timestamp == null) {
            return jsonParsers.getLocale("giveaway_edit_ends", guildId) + " N/A";
        } else {
            long time = timestamp.getTime() / 1000;
            return String.format(jsonParsers.getLocale("giveaway_data_end", guildId), time, time);
        }
    }

    private void removeScheduling(String giveawayId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeScheduling(giveawayId);

        schedulingRepository.deleteByIdSalt(giveawayId);
    }

    private void removeActiveGiveaway(long messageId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeGiveaway(messageId);

        activeGiveawayRepository.deleteByMessageId(messageId);
    }
}