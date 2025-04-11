package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Participants;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ParticipantsPaginationHandlerButton {

    private final ParticipantsRepository participantsRepository;
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();

        if (event.getButton().getId() == null) {
            event.editMessage("Options is null").queue();
            return;
        }

        //[NEXT, 1359511082565238914, 1]
        String[] split = event.getButton().getId().split("_");

        long giveawayIdLong = Long.parseLong(split[1]);
        String giveawayId = split[1];
        int page = Integer.parseInt(split[2]);

        Pageable pageable = PageRequest.of(page, 10); // первая страница, 10 элементов
        Page<Participants> participantsPage = participantsRepository.findAllByMessageId(giveawayIdLong, pageable);

        long total = participantsPage.getTotalElements();

        StringBuilder stringBuilder = new StringBuilder();
        List<Participants> content = participantsPage.getContent();
        String totalPages = String.valueOf(participantsPage.getTotalPages());

        int startIndex;

        if (page == 0) startIndex = 0;
        else startIndex = (page + 1) + 10; //content.size()

        for (int i = 0; i < content.size(); i++) {
            Participants participants = content.get(i);
            Long userId = participants.getUserId();
            stringBuilder.append(startIndex + i + 1).append(". ").append("<@!").append(userId).append("> ").append("\n");
        }

        if (stringBuilder.isEmpty()) {
            String paginationNoParticipants = jsonParsers.getLocale("pagination_no_participants", guildId);
            event.editMessage(paginationNoParticipants).queue();
        } else {
            List<Button> buttons = new ArrayList<>();

            if (participantsPage.hasPrevious()) {
                Button nextButton = Button.secondary("NEXT_".concat(giveawayId).concat("_").concat(String.valueOf(page - 1)), Emoji.fromUnicode("⬅️")); // 1 — следующая страница
                buttons.add(nextButton);
            }

            Button button = Button.secondary("DOWNLOAD_".concat(giveawayId), "Скачать");
            buttons.add(button);

            if (participantsPage.hasNext()) {
                Button nextButton = Button.secondary("NEXT_".concat(giveawayId).concat("_").concat(String.valueOf(page + 1)), Emoji.fromUnicode("➡️")); // 1 — следующая страница
                buttons.add(nextButton);
            }

            String paginationPage = jsonParsers.getLocale("pagination_page", guildId);
            String paginationParticipantsCount = jsonParsers.getLocale("pagination_participants_count", guildId);

            SelfUser selfUser = event.getJDA().getSelfUser();
            String avatarUrl = selfUser.getAvatarUrl();
            String name = selfUser.getName().concat(" ").concat("(").concat(paginationPage).concat(String.valueOf(page + 1)).concat("/").concat(totalPages).concat(")");
            Color userColor = GiveawayUtils.getUserColor(guildId);

            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setColor(userColor);
            embedBuilder.setAuthor(name, null, avatarUrl);
            embedBuilder.setDescription(stringBuilder.toString());
            embedBuilder.setFooter(paginationParticipantsCount.concat(String.valueOf(total)));

            event.editMessageEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
        }
    }
}