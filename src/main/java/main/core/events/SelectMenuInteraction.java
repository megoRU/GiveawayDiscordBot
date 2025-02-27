package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class SelectMenuInteraction {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;

    public void handle(@NotNull StringSelectInteractionEvent event) {
        long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        if (event.getSelectedOptions().isEmpty()) {
            String selectMenuNotSelect = jsonParsers.getLocale("select_menu_not_select", guildId);
            event.reply(selectMenuNotSelect).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) return;
        boolean isUserHasManageServer = member.getPermissions().contains(Permission.MANAGE_SERVER);

        if (!isUserHasManageServer) {
            String userDontHasPermission = jsonParsers.getLocale("user_dont_has_permission", guildId);
            event.reply(userDontHasPermission).setEphemeral(true).queue();
            return;
        }

        String selectedValue = event.getSelectedOptions().getFirst().getValue();
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
            String selectMenuError = jsonParsers.getLocale("select_menu_error", guildId);
            event.reply(selectMenuError).queue();
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
            String selectMenuGiveawayNotFound = jsonParsers.getLocale("select_menu_giveaway_not_found", guildId);
            event.reply(selectMenuGiveawayNotFound).queue();
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
            String selectMenuSchedulingNotFound = jsonParsers.getLocale("select_menu_scheduling_not_found", guildId);
            event.reply(selectMenuSchedulingNotFound).queue();
        }
    }

    private void handleStopGiveaway(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replace("stop_", "");
        Giveaway giveaway = instance.getGiveaway(Long.parseLong(messageId));

        if (giveaway != null) {
            giveaway.stopGiveaway(giveaway.getGiveawayData().getCountWinners());
            handleBackSelection(event, guildId, instance);
        } else {
            event.editMessage(jsonParsers.getLocale("giveaway_not_found_by_id", guildId)).queue();
        }
    }

    private void handleBackSelection(StringSelectInteractionEvent event, long guildId, GiveawayRegistry instance) {
        List<Scheduling> schedulingList = instance.getSchedulingByGuild(guildId);
        List<Giveaway> giveawayList = instance.getGiveawaysByGuild(guildId);

        // Формируем сообщение
        String formatListMessage = ListCommand.formatListMessage(schedulingList, giveawayList, guildId);
        var menuBuilder = ListCommand.formatListMenuMessage(schedulingList, giveawayList, guildId);

        if (menuBuilder.getOptions().isEmpty()) {
            event.editMessage(formatListMessage).setComponents().queue();
        } else {
            var menu = menuBuilder.build();
            var actionRow = ActionRow.of(menu);
            event.editMessage(formatListMessage).setComponents(actionRow).queue();
        }
    }

    private void handleCancelSelection(StringSelectInteractionEvent event, String selectedValue, long guildId, GiveawayRegistry instance) {
        String messageId = selectedValue.replaceAll("cancel_", "");

        if (messageId.matches("[0-9]+")) {
            long messageIdLong = Long.parseLong(messageId);
            Giveaway giveaway = instance.getGiveaway(messageIdLong);

            if (giveaway == null) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeActiveGiveaway(messageIdLong);
                handleBackSelection(event, guildId, instance);
            }
        } else {
            Scheduling scheduling = instance.getScheduling(messageId);

            if (scheduling == null) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeScheduling(messageId);
                handleBackSelection(event, guildId, instance);
            }
        }
    }

    private String formatGiveawayMessage(Giveaway giveaway, long guildId) {
        GiveawayData giveawayData = giveaway.getGiveawayData();
        String title = giveawayData.getTitle();
        Long roleId = giveawayData.getRoleId();
        int countWinners = giveawayData.getCountWinners();
        int minParticipants = giveawayData.getMinParticipants();
        Timestamp endGiveawayDate = giveawayData.getEndGiveawayDate();
        String urlImage = giveawayData.getUrlImage();

        String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
        String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
        String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);
        String listMenuParticipants = String.format(jsonParsers.getLocale("list_menu_participants", guildId), roleId);

        return "**🎉 Giveaway:**\n" +
                giveawayEditTitle + " " + title + "\n" +
                giveawayEditWinners + " " + countWinners + "\n" +
                (roleId != null ? giftOnlyFor + "\n" : "") +
                listMenuParticipants + minParticipants + "\n" +
                getDateTranslation(endGiveawayDate, guildId) + "\n" +
                (urlImage != null ? urlImage : "");
    }

    private String formatSchedulingMessage(Scheduling scheduling, long guildId) {
        Long roleId = scheduling.getRoleId();
        String title = scheduling.getTitle();
        int countWinners = scheduling.getCountWinners();
        Timestamp dateEnd = scheduling.getDateEnd();
        String urlImage = scheduling.getUrlImage();
        Timestamp dateCreateGiveaway = scheduling.getDateCreateGiveaway();

        String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
        String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
        String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);

        return "**📅 Scheduling:**\n" +
                giveawayEditTitle + " " + title + "\n" +
                giveawayEditWinners + " " + countWinners + "\n" +
                (roleId != null ? giftOnlyFor + "\n" : "") +
                getDateTranslation(dateEnd, guildId) + "\n" +
                getDateStartTranslation(dateCreateGiveaway, guildId) + "\n" +
                (urlImage != null ? urlImage : "");
    }

    private StringSelectMenu createGiveawayMenu(Giveaway giveaway) {
        long guildId = giveaway.getGuildId();

        String selectMenuBack = jsonParsers.getLocale("select_menu_back", guildId);
        String selectMenuCancel = jsonParsers.getLocale("select_menu_cancel", guildId);
        String selectMenuStop = jsonParsers.getLocale("select_menu_stop", guildId);

        return StringSelectMenu.create("select_action")
                .addOption(selectMenuStop, "stop_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("🎉"))
                .addOption(selectMenuCancel, "cancel_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("❌"))
                .addOption(selectMenuBack, "back_" + giveaway.getGiveawayData().getMessageId(), Emoji.fromUnicode("⬅️"))
                .build();
    }

    private StringSelectMenu createSchedulingMenu(Scheduling scheduling) {
        Long guildId = scheduling.getGuildId();
        String selectMenuCancel = jsonParsers.getLocale("select_menu_cancel", guildId);
        String selectMenuBack = jsonParsers.getLocale("select_menu_back", guildId);

        return StringSelectMenu.create("select_action")
                .addOption(selectMenuCancel, "cancel_" + scheduling.getIdSalt(), Emoji.fromUnicode("❌"))
                .addOption(selectMenuBack, "back_" + scheduling.getIdSalt(), Emoji.fromUnicode("⬅️"))
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

    private String getDateStartTranslation(Timestamp timestamp, long guildId) {
        if (timestamp == null) {
            return jsonParsers.getLocale("giveaway_edit_start", guildId) + " N/A";
        } else {
            long time = timestamp.getTime() / 1000;
            return String.format(jsonParsers.getLocale("giveaway_data_start", guildId), time, time);
        }
    }

    private void removeScheduling(String giveawayId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeScheduling(giveawayId);

        schedulingRepository.deleteByIdSalt(giveawayId);
    }

    private void removeActiveGiveaway(long messageId) {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Giveaway giveaway = instance.getGiveaway(messageId);
        instance.removeGiveaway(messageId);

        activeGiveawayRepository.deleteByMessageId(messageId);
        if (giveaway != null) giveaway.cancelGiveaway();
    }
}