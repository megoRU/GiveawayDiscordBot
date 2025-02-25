package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.model.entity.Scheduling;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ListCommand {

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);

        // Формируем сообщение
        String message = "**🎉 Активные Giveaway:**\n";
        message += giveawayList.isEmpty() ? "Нет активных Giveaway.\n" : giveawayList.stream()
                .map(g -> "- " + g.getGiveawayData().getTitle() + " | `" + g.getGiveawayData().getMessageId() + "`")
                .collect(Collectors.joining("\n")) + "\n";

        message += "\n**📅 Запланированные Giveaway:**\n";
        message += schedulingList.isEmpty() ? "Нет запланированных Giveaway.\n" : schedulingList.stream()
                .map(s -> "- " + s.getTitle() +  " | `" + s.getIdSalt() + "`")
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
            event.reply(message).queue();
        } else {
            var menu = menuBuilder.build();
            var actionRow = ActionRow.of(menu);
            event.reply(message).setComponents(actionRow).queue();
        }
    }
}