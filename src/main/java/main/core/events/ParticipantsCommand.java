package main.core.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ListUsers;
import main.model.entity.Participants;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
public class ParticipantsCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final static GiveawayRegistry instance = GiveawayRegistry.getInstance();
    private final ListUsersRepository listUsersRepository;
    private final ParticipantsRepository participantsRepository;

    @Transactional
    public void participants(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        long guildId = event.getGuild().getIdLong();
        long userIdLong = event.getInteraction().getUser().getIdLong();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (id != null) {
            Pagination pagination = getParticipants(userIdLong, Long.parseLong(id), 0, participantsRepository, listUsersRepository);
            Page<?> page = pagination.getPage();
            List<String> participantsCollection = pagination.getParticipants();

            if (page.isEmpty()) {
                String paginationUserIdError = jsonParsers.getLocale("pagination_user_id_error", guildId);
                event.reply(paginationUserIdError).setEphemeral(true).queue();
                return;
            }

            long total = page.getTotalElements();
            String totalPages = String.valueOf(page.getTotalPages());

            StringBuilder stringBuilder = new StringBuilder();


            for (int i = 0; i < participantsCollection.size(); i++) {
                Long userId = Long.valueOf(participantsCollection.get(i));
                stringBuilder.append(i + 1).append(". ").append("<@!").append(userId).append("> ").append("\n");
            }

            String paginationDownload = jsonParsers.getLocale("pagination_download", guildId);

            Button button = Button.secondary("DOWNLOAD_".concat(id), paginationDownload);
            List<Button> buttons = new ArrayList<>();
            buttons.add(button);

            if (page.hasNext()) {
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

            event.replyEmbeds(embedBuilder.build()).setComponents(ActionRow.of(buttons)).queue();
        } else {
            event.reply("Options is null").queue();
        }
    }

    public static Pagination getParticipants(long userIdLong, long giveawayId, int page,
                                             ParticipantsRepository participantsRepository,
                                             ListUsersRepository listUsersRepository) {
        Giveaway giveaway = instance.getGiveaway(giveawayId);
        Collection<String> collection = new ArrayList<>();

        Pagination.PaginationBuilder builder = Pagination.builder();

        Pageable pageable = PageRequest.of(page, 10); // первая страница, 10 элементов

        if (giveaway != null) {
            Page<Participants> participantsPage = participantsRepository.findAllByMessageId(giveawayId, userIdLong, pageable);

            builder.page(participantsPage);

            List<Participants> content = participantsPage.getContent();
            for (Participants participants : content) {
                Long userId = participants.getUserId();
                collection.add(userId.toString());
            }
        } else {
            Page<ListUsers> participantsPage = listUsersRepository.findAllByMessageId(giveawayId, userIdLong, pageable);
            List<ListUsers> content = participantsPage.getContent();

            builder.page(participantsPage);

            for (ListUsers participants : content) {
                Long userId = participants.getUserId();
                collection.add(String.valueOf(userId));
            }

        }

        return builder.participants(collection.stream().toList()).build();
    }

    @Builder
    @Getter
    public static class Pagination {
        private Page<?> page;
        private List<String> participants;
    }
}