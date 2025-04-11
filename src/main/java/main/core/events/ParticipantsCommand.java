package main.core.events;

import lombok.AllArgsConstructor;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Participants;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ParticipantsCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ParticipantsRepository participantsRepository;

    @Transactional
    public void participants(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (id != null) {
            Pageable pageable = PageRequest.of(0, 1); // первая страница, 10 элементов
            Page<Participants> participantsPage = participantsRepository.findAllByMessageId(Long.parseLong(id), pageable);
            long total = participantsPage.getTotalElements();
            String totalPages = String.valueOf(participantsPage.getTotalPages());

            StringBuilder stringBuilder = new StringBuilder();
            List<Participants> content = participantsPage.getContent();

            for (int i = 0; i < content.size(); i++) {
                Participants participants = content.get(i);
                Long userId = participants.getUserId();
                stringBuilder.append(i + 1).append(". ").append("<@!").append(userId).append("> ").append("\n");
            }

            if (stringBuilder.isEmpty()) {
                String paginationNoParticipants = jsonParsers.getLocale("pagination_no_participants", guildId);
                event.reply(paginationNoParticipants).queue();
            } else {
                Button button = Button.secondary("DOWNLOAD_".concat(id), "Скачать");
                List<Button> buttons = new ArrayList<>();
                buttons.add(button);

                if (participantsPage.hasNext()) {
                    Button nextButton = Button.secondary("NEXT_".concat(id).concat("_1"), Emoji.fromUnicode("➡️"));
                    buttons.add(nextButton);
                }

                String paginationPage = jsonParsers.getLocale("pagination_page", guildId);
                String paginationParticipantsCount = jsonParsers.getLocale("pagination_participants_count", guildId);

                SelfUser selfUser = event.getJDA().getSelfUser();
                String avatarUrl = selfUser.getAvatarUrl();
                String name = selfUser.getName().concat(" ").concat("(").concat(paginationPage).concat("1/").concat(totalPages).concat(")");
                Color userColor = GiveawayUtils.getUserColor(guildId);

                EmbedBuilder embedBuilder = new EmbedBuilder();

                embedBuilder.setColor(userColor);
                embedBuilder.setAuthor(name, null, avatarUrl);
                embedBuilder.setDescription(stringBuilder.toString());
                embedBuilder.setFooter(paginationParticipantsCount + total);

                event.replyEmbeds(embedBuilder.build()).setActionRow(buttons).queue();
            }
        } else {
            event.reply("Options is null").queue();
        }
    }
}