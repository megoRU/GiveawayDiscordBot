package main.core.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.ListUsers;
import main.model.entity.Participants;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@AllArgsConstructor
public class ParticipantsCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ListUsersRepository listUsersRepository;
    private final ParticipantsRepository participantsRepository;

    @Transactional
    public void participants(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (id != null) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            if (id.matches("[0-9]+")) {
                long giveawayId = Long.parseLong(id);

                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                Giveaway giveaway = instance.getGiveaway(giveawayId);

                if (giveaway != null) {
                    List<ActiveGiveaways> giveawaysWithParticipants = participantsRepository.findActiveGiveawaysWithParticipants(giveawayId, userIdLong);
                    Set<Participants> participants = giveawaysWithParticipants.getFirst().getParticipants();

                    List<ListUsers> listUsers = new ArrayList<>();

                    participants.forEach(participant -> {
                        String nickName = participant.getNickName();
                        Long userId = participant.getUserId();
                        Long createdUserId = participant.getActiveGiveaways().getCreatedUserId();
                        Long guildIdFrom = participant.getActiveGiveaways().getGuildId();

                        ListUsers listUser = new ListUsers();
                        listUser.setUserId(userId);
                        listUser.setNickName(nickName);
                        listUser.setGuildId(guildIdFrom);
                        listUser.setGiveawayId(giveawayId);
                        listUser.setCreatedUserId(createdUserId);
                        listUsers.add(listUser);
                    });

                    sendParticipants(event, guildId, participants.isEmpty(), gson.toJson(listUsers));
                } else {
                    List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayIdAndCreatedUserId(Long.parseLong(id), userIdLong);
                    sendParticipants(event, guildId, listUsers.isEmpty(), gson.toJson(listUsers));
                }
            } else {
                String slashStopNoHas = jsonParsers.getLocale("id_must_be_a_number", guildId);
                event.getHook().sendMessage(slashStopNoHas).setEphemeral(true).queue();
            }
        } else {
            event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
        }
    }


    private void sendParticipants(@NotNull SlashCommandInteractionEvent event, long guildId, boolean empty, String json) {
        if (empty) {
            String noAccessReroll = jsonParsers.getLocale("no_access_reroll", guildId);
            event.getHook().sendMessage(noAccessReroll).setEphemeral(true).queue();
        } else {
            InputStream inputStream = getInputStream(json);
            FileUpload fileUpload = FileUpload.fromData(inputStream, "participants.json");
            event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
        }
    }

    private InputStream getInputStream(@NotNull String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}