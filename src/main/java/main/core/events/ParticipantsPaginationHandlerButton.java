package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ParticipantsPaginationHandlerButton {

    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        long userIdLong = event.getInteraction().getUser().getIdLong();

        if (event.getButton().getId() == null) {
            event.editMessage("Options is null").queue();
            return;
        }

        //[NEXT, 1359511082565238914, 1]
        String[] split = event.getButton().getId().split("_");

        long giveawayIdLong = Long.parseLong(split[1]);
        String giveawayId = split[1];
        int page = Integer.parseInt(split[2]);

        ParticipantsCommand.Pagination pagination = ParticipantsCommand.getParticipants(userIdLong, giveawayIdLong, page, participantsRepository, listUsersRepository);
        Page<?> paginationPage = pagination.getPage();
        List<String> participantsCollection = pagination.getParticipants();

        if (paginationPage.isEmpty()) {
            String paginationUserIdError = jsonParsers.getLocale("pagination_user_id_error", guildId);
            event.reply(paginationUserIdError).setActionRow().queue();
            return;
        }

        long total = paginationPage.getTotalElements();

        StringBuilder stringBuilder = new StringBuilder();
        String totalPages = String.valueOf(paginationPage.getTotalPages());

        int startIndex;

        if (page == 0) startIndex = 0;
        else startIndex = page * 10;

        for (int i = 0; i < participantsCollection.size(); i++) {
            Long userId = Long.valueOf(participantsCollection.get(i));
            stringBuilder.append(startIndex + i + 1).append(". ").append("<@!").append(userId).append("> ").append("\n");
        }

        List<Button> buttons = new ArrayList<>();

        if (paginationPage.hasPrevious()) {
            Button nextButton = Button.secondary("NEXT_".concat(giveawayId).concat("_").concat(String.valueOf(page - 1)), Emoji.fromUnicode("⬅️")); // 1 — следующая страница
            buttons.add(nextButton);
        }

        String paginationDownload = jsonParsers.getLocale("pagination_download", guildId);

        Button button = Button.secondary("DOWNLOAD_".concat(giveawayId), paginationDownload);
        buttons.add(button);

        if (paginationPage.hasNext()) {
            Button nextButton = Button.secondary("NEXT_".concat(giveawayId).concat("_").concat(String.valueOf(page + 1)), Emoji.fromUnicode("➡️")); // 1 — следующая страница
            buttons.add(nextButton);
        }

        String paginationPageTranslation = jsonParsers.getLocale("pagination_page", guildId);
        String paginationParticipantsCount = jsonParsers.getLocale("pagination_participants_count", guildId);

        SelfUser selfUser = event.getJDA().getSelfUser();
        String avatarUrl = selfUser.getAvatarUrl();
        String name = selfUser.getName().concat(" ").concat("(").concat(paginationPageTranslation).concat(String.valueOf(page + 1)).concat("/").concat(totalPages).concat(")");
        Color userColor = GiveawayUtils.getUserColor(guildId);

        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setColor(userColor);
        embedBuilder.setAuthor(name, null, avatarUrl);
        embedBuilder.setDescription(stringBuilder.toString());
        embedBuilder.setFooter(paginationParticipantsCount.concat(String.valueOf(total)));

        event.editMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
    }
}