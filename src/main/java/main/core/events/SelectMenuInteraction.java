package main.core.events;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.Objects;

public class SelectMenuInteraction {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull StringSelectInteractionEvent event) {
        String selectedValue = event.getSelectedOptions().getFirst().getValue(); // Получаем выбранное значение
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (selectedValue.startsWith("giveaway_")) {
            String messageId = selectedValue.replace("giveaway_", "");

            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Giveaway giveaway = instance.getGiveaway(Long.parseLong(messageId));

            if (giveaway != null) {
                Timestamp endGiveawayDate = giveaway.getGiveawayData().getEndGiveawayDate();
                // Формируем новое сообщение с подробной информацией о Giveaway
                String message = "**🎉 Giveaway:**\n";
                message += "Название: " + giveaway.getGiveawayData().getTitle() + "\n";
                message += getDateTranslation(endGiveawayDate, guildId) + "\n"; // Например, дата окончания

                // Создаём меню для взаимодействия, если нужно добавить новые опции
                var menuBuilder = StringSelectMenu.create("select_action");
                menuBuilder.addOption("Разыграть", "stop_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("🎉"));
                menuBuilder.addOption("Отменить", "cancel_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("❌"));
                menuBuilder.addOption("Назад", "back_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("⬅️"));

                var menu = menuBuilder.build();
                var actionRow = ActionRow.of(menu);

                // Редактируем сообщение
                event.editMessage(message).setComponents(actionRow).queue();
            } else {
                event.reply("Giveaway с таким ID не найден!").queue();
            }
        } else {
            // Обработка других случаев выбора
            event.reply("Неизвестный выбор.").queue();
        }
    }

    private String getDateTranslation(@Nullable Timestamp timestamp, long guildId) {
        if (timestamp == null) {
            String giveawayEditEnds = jsonParsers.getLocale("giveaway_edit_ends", guildId);
            return giveawayEditEnds + " N/A";
        } else {
            long time = timestamp.getTime() / 1000;
            return String.format(jsonParsers.getLocale("giveaway_data_end", guildId), time, time);
        }
    }
}
