package main.core.events;

import lombok.AllArgsConstructor;
import main.controller.UpdateController;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Scheduling;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.service.GiveawayRepositoryService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class SelectMenuInteraction {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final UpdateController updateController;

    @Transactional
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

        if (selectedValue.startsWith("giveaway_")) {
            handleGiveawaySelection(event, selectedValue, guildId);
        } else if (selectedValue.startsWith("scheduling_")) {
            handleSchedulingSelection(event, selectedValue, guildId);
        } else if (selectedValue.startsWith("stop_")) {
            handleStopGiveaway(event, selectedValue, guildId);
        } else if (selectedValue.startsWith("back_")) {
            handleBackSelection(event, guildId);
        } else if (selectedValue.startsWith("cancel_")) {
            handleCancelSelection(event, selectedValue, guildId);
        } else {
            String selectMenuError = jsonParsers.getLocale("select_menu_error", guildId);
            event.reply(selectMenuError).queue();
        }
    }

    private void handleGiveawaySelection(StringSelectInteractionEvent event, String selectedValue, long guildId) {
        String messageId = selectedValue.replace("giveaway_", "");
        ActiveGiveaways activeGiveaway = activeGiveawayRepository.findByMessageId(Long.parseLong(messageId));

        if (activeGiveaway != null && activeGiveaway.getGuildId().equals(guildId)) {
            String message = formatGiveawayMessage(activeGiveaway, guildId);
            var menu = createGiveawayMenu(activeGiveaway);
            event.editMessage(message).setComponents(ActionRow.of(menu)).queue();
        } else {
            String selectMenuGiveawayNotFound = jsonParsers.getLocale("select_menu_giveaway_not_found", guildId);
            event.reply(selectMenuGiveawayNotFound).queue();
        }
    }

    private void handleSchedulingSelection(StringSelectInteractionEvent event, String selectedValue, long guildId) {
        String messageId = selectedValue.replace("scheduling_", "");
        Scheduling scheduling = schedulingRepository.findByIdSalt(messageId);

        if (scheduling != null && scheduling.getGuildId().equals(guildId)) {
            String message = formatSchedulingMessage(scheduling, guildId);
            var menu = createSchedulingMenu(scheduling);
            event.editMessage(message).setComponents(ActionRow.of(menu)).queue();
        } else {
            String selectMenuSchedulingNotFound = jsonParsers.getLocale("select_menu_scheduling_not_found", guildId);
            event.reply(selectMenuSchedulingNotFound).queue();
        }
    }

    private void handleStopGiveaway(StringSelectInteractionEvent event, String selectedValue, long guildId) {
        String messageId = selectedValue.replace("stop_", "");
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByMessageId(Long.parseLong(messageId));

        if (activeGiveaways != null && activeGiveaways.getGuildId().equals(guildId)) {
            int countWinners = activeGiveaways.getCountWinners();
            Giveaway giveaway = new Giveaway(giveawayRepositoryService, updateController);

            giveaway.stopGiveaway(activeGiveaways, countWinners);

            handleBackSelection(event, guildId);
        } else {
            event.editMessage(jsonParsers.getLocale("giveaway_not_found_by_id", guildId)).queue();
        }
    }

    private void handleBackSelection(StringSelectInteractionEvent event, long guildId) {
        List<Scheduling> schedulingList = schedulingRepository.findByGuildId(guildId);
        List<ActiveGiveaways> giveawayList = activeGiveawayRepository.findByGuildId(guildId);
        if (giveawayList == null) giveawayList = Collections.emptyList();

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

    private void handleCancelSelection(StringSelectInteractionEvent event, String selectedValue, long guildId) {
        String messageId = selectedValue.replace("cancel_", "");

        if (messageId.matches("[0-9]+")) {
            long messageIdLong = Long.parseLong(messageId);
            ActiveGiveaways activeGiveaways = activeGiveawayRepository.findByMessageId(messageIdLong);

            if (activeGiveaways == null || !activeGiveaways.getGuildId().equals(guildId)) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeActiveGiveaway(activeGiveaways);
                handleBackSelection(event, guildId);
            }
        } else {
            Scheduling scheduling = schedulingRepository.findByIdSalt(messageId);

            if (scheduling == null || !scheduling.getGuildId().equals(guildId)) {
                String giveawayNotFoundById = jsonParsers.getLocale("giveaway_not_found_by_id", guildId);
                event.editMessage(giveawayNotFoundById).setComponents().queue();
            } else {
                removeScheduling(messageId);
                handleBackSelection(event, guildId);
            }
        }
    }

    private String formatGiveawayMessage(ActiveGiveaways activeGiveaways, long guildId) {
        String title = activeGiveaways.getTitle() == null ? "Giveaway" : activeGiveaways.getTitle();
        long userIdLong = activeGiveaways.getCreatedUserId();
        Long roleId = activeGiveaways.getRoleId();
        int countWinners = activeGiveaways.getCountWinners();
        int minParticipants = activeGiveaways.getMinParticipants() == null ? 1 : activeGiveaways.getMinParticipants();
        Instant endGiveawayDate = activeGiveaways.getEndGiveawayDate();
        String urlImage = activeGiveaways.getUrlImage();

        String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
        String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
        String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);
        String listMenuParticipants = String.format(jsonParsers.getLocale("list_menu_participants", guildId), roleId);

        return "**🎉 Giveaway:**\n" +
                giveawayEditTitle + " " + title + "\n" +
                giveawayEditWinners + " " + countWinners + "\n" +
                (roleId != null ? giftOnlyFor + "\n" : "") +
                listMenuParticipants + minParticipants + "\n" +
                getDateTranslation(endGiveawayDate, guildId, userIdLong) + "\n" +
                (urlImage != null ? urlImage : "");
    }

    private String formatSchedulingMessage(Scheduling scheduling, long guildId) {
        Long roleId = scheduling.getRoleId();
        String title = scheduling.getTitle();
        int countWinners = scheduling.getCountWinners();
        Long createdUserId = scheduling.getCreatedUserId();
        Instant dateEnd = scheduling.getDateEndGiveaway();
        String urlImage = scheduling.getUrlImage();
        Instant dateCreateGiveaway = scheduling.getDateCreateGiveaway();

        String giveawayEditTitle = jsonParsers.getLocale("giveaway_edit_title", guildId);
        String giveawayEditWinners = jsonParsers.getLocale("giveaway_edit_winners", guildId);
        String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildId), roleId);

        return "**📅 Scheduling:**\n" +
                giveawayEditTitle + " " + title + "\n" +
                giveawayEditWinners + " " + countWinners + "\n" +
                (roleId != null ? giftOnlyFor + "\n" : "") +
                getDateTranslation(dateEnd, guildId, createdUserId) + "\n" +
                getDateStartTranslation(dateCreateGiveaway, guildId, createdUserId) + "\n" +
                (urlImage != null ? urlImage : "");
    }

    private StringSelectMenu createGiveawayMenu(ActiveGiveaways activeGiveaways) {
        long guildId = activeGiveaways.getGuildId();

        String selectMenuBack = jsonParsers.getLocale("select_menu_back", guildId);
        String selectMenuCancel = jsonParsers.getLocale("select_menu_cancel", guildId);
        String selectMenuStop = jsonParsers.getLocale("select_menu_stop", guildId);

        return StringSelectMenu.create("select_action")
                .addOption(selectMenuStop, "stop_" + activeGiveaways.getMessageId(), Emoji.fromUnicode("🎉"))
                .addOption(selectMenuCancel, "cancel_" + activeGiveaways.getMessageId(), Emoji.fromUnicode("❌"))
                .addOption(selectMenuBack, "back_" + activeGiveaways.getMessageId(), Emoji.fromUnicode("⬅️"))
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

    private String getDateTranslation(Instant endGiveawayDate, long guildId, long userId) {
        long time = GiveawayUtils.getEpochSecond(endGiveawayDate, userId);
        return String.format(jsonParsers.getLocale("giveaway_data_end", guildId), time, time);
    }

    private String getDateStartTranslation(Instant localDateTime, long guildId, long userId) {
        if (localDateTime == null) {
            return jsonParsers.getLocale("giveaway_edit_start", guildId) + " N/A";
        } else {
            long time = GiveawayUtils.getEpochSecond(localDateTime, userId);
            return String.format(jsonParsers.getLocale("giveaway_data_start", guildId), time, time);
        }
    }

    private void removeScheduling(String giveawayId) {
        schedulingRepository.deleteByIdSalt(giveawayId);
    }

    private void removeActiveGiveaway(ActiveGiveaways activeGiveaways) {
        Giveaway giveaway = new Giveaway(giveawayRepositoryService, updateController);
        giveaway.cancelGiveaway(activeGiveaways);
    }
}