package main.core.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.ListUsers;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ParticipantsDownloadButton {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ListUsersRepository listUsersRepository;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JSONParsers jsonParsers = new JSONParsers();

    public void handle(@NotNull ButtonInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();

        if (event.getButton().getCustomId() == null) {
            event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
            return;
        }

        String id = event.getButton().getCustomId().replace("DOWNLOAD_", "");

        if (id.matches("[0-9]+")) {
            long giveawayId = Long.parseLong(id);

            ActiveGiveaways activeGiveaway = activeGiveawayRepository.findByMessageId(giveawayId);

            if (activeGiveaway != null) {
                Set<Long> participantsList = activeGiveaway.getParticipants() != null ? activeGiveaway.getParticipants()
                        .stream()
                        .map(Participants::getUserId)
                        .collect(Collectors.toSet()) : Set.of();
                long creatorUserId = activeGiveaway.getCreatedUserId();

                if (userIdLong != creatorUserId) {
                    String noAccessReroll = jsonParsers.getLocale("no_access_reroll", guildId);
                    event.getHook().sendMessage(noAccessReroll).setEphemeral(true).queue();
                    return;
                }

                List<ListUsers> listUsers = new ArrayList<>();

                participantsList.forEach(participant -> {
                    ListUsers listUser = new ListUsers();
                    listUser.setUserId(participant);

                    listUsers.add(listUser);
                });

                sendParticipants(event, gson.toJson(listUsers));
            } else {
                List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayIdAndCreatedUserId(Long.parseLong(id), userIdLong);
                sendParticipants(event, gson.toJson(listUsers));
            }
        } else {
            String slashStopNoHas = jsonParsers.getLocale("id_must_be_a_number", guildId);
            event.getHook().sendMessage(slashStopNoHas).setEphemeral(true).queue();
        }
    }

    private void sendParticipants(@NotNull ButtonInteractionEvent event, String json) {
        InputStream inputStream = getInputStream(json);
        FileUpload fileUpload = FileUpload.fromData(inputStream, "participants.json");
        event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
    }

    private InputStream getInputStream(@NotNull String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}