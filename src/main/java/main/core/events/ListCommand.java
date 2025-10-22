package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ListCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);

        // Формируем сообщение
        String formatListMessage = formatListMessage(schedulingList, giveawayList, guildId);
        var menuBuilder = formatListMenuMessage(schedulingList, giveawayList, guildId);

        if (menuBuilder.getOptions().isEmpty()) {
            event.reply(formatListMessage).queue();
        } else {
            var menu = menuBuilder.build();
            var actionRow = ActionRow.of(menu);
            event.reply(formatListMessage).setComponents(actionRow).queue();
        }
    }

    public static String formatTitle(String title) {
        if (title == null || title.length() <= 30) {
            return title;
        }
        return title.substring(0, 30) + "...";
    }

    public static String formatListMessage(List<Scheduling> schedulingList, List<Giveaway> giveawayList, long guildId) {
        // Формируем сообщение
        String listNoActiveGiveaway = jsonParsers.getLocale("list_no_active_giveaway", guildId);
        String listNoSchedulingGiveaway = jsonParsers.getLocale("list_no_scheduling_giveaway", guildId);

        String message = jsonParsers.getLocale("list_active_giveaway", guildId);
        message += giveawayList.isEmpty() ? listNoActiveGiveaway : giveawayList.stream()
                .map(g -> "- " + formatTitle(g.getGiveawayData().getTitle()) + " | `" + g.getGiveawayData().getMessageId() + "`")
                .collect(Collectors.joining("\n")) + "\n";

        message += jsonParsers.getLocale("list_scheduling_giveaway", guildId);
        message += schedulingList.isEmpty() ? listNoSchedulingGiveaway : schedulingList.stream()
                .map(s -> "- " + formatTitle(s.getTitle()) + " | `" + s.getIdSalt() + "`")
                .collect(Collectors.joining("\n"));

        return message;
    }

    public static StringSelectMenu.Builder formatListMenuMessage(List<Scheduling> schedulingList, List<Giveaway> giveawayList, long guildId) {
        var menuBuilder = StringSelectMenu.create("select_action");
        String listMenuViewer = jsonParsers.getLocale("list_menu_viewer", guildId);

        giveawayList.forEach(g ->
                menuBuilder.addOption(
                        formatTitle(g.getGiveawayData().getTitle()),
                        "giveaway_" + g.getGiveawayData().getMessageId(),
                        listMenuViewer + g.getGiveawayData().getMessageId()

                )
        );

        schedulingList.forEach(s ->
                menuBuilder.addOption(
                        formatTitle(s.getTitle()),
                        "scheduling_" + s.getIdSalt(),
                        listMenuViewer + s.getIdSalt()
                ));

        return menuBuilder;
    }
}