package main.core.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.JSONParsers;
import main.model.entity.ListUsers;
import main.model.repository.ListUsersRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ParticipantsCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ListUsersRepository listUsersRepository;
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Transactional
    public void participants(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (id != null) {
            if (id.matches("[0-9]+")) {
                long giveawayId = Long.parseLong(id);

                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                Giveaway giveaway = instance.getGiveaway(giveawayId);

                if (giveaway != null) {
                    GiveawayData giveawayData = giveaway.getGiveawayData();
                    List<Long> participantsList = giveawayData.getParticipantsList().values().stream().map(Long::valueOf).toList();
                    long creatorUserId = giveaway.getUserIdLong();

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

                    handlerMessageSender(event, listUsers);
                } else {
                    List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayIdAndCreatedUserId(Long.parseLong(id), userIdLong);
                    handlerMessageSender(event, listUsers);
                }
            } else {
                String slashStopNoHas = jsonParsers.getLocale("id_must_be_a_number", guildId);
                event.getHook().sendMessage(slashStopNoHas).setEphemeral(true).queue();
            }
        } else {
            event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
        }
    }

    private void sendParticipants(@NotNull SlashCommandInteractionEvent event, String json) {
        InputStream inputStream = getInputStream(json);
        FileUpload fileUpload = FileUpload.fromData(inputStream, "participants.json");
        event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
    }

    private void handlerMessageSender(@NotNull SlashCommandInteractionEvent event, List<ListUsers> listUsers) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        StringBuilder stringBuilder = new StringBuilder();

        if (listUsers.isEmpty()) {
            String noAccessReroll = jsonParsers.getLocale("no_access_reroll", guildId);
            event.getHook().sendMessage(noAccessReroll).setEphemeral(true).queue();
            return;
        }

        for (ListUsers participants : listUsers) {
            stringBuilder.append(stringBuilder.isEmpty() ? "<@" : ", <@").append(participants.getUserId()).append(">");
        }

        if (stringBuilder.length() > 1) {
            sendParticipants(event, gson.toJson(listUsers));
        } else {
            String participants = jsonParsers.getLocale("participants", guildId);

            EmbedBuilder list = new EmbedBuilder();
            list.setColor(Color.GREEN);
            list.setTitle(participants);
            list.setDescription(stringBuilder);

            event.getHook().sendMessageEmbeds(list.build()).setEphemeral(true).queue();
        }
    }

    private InputStream getInputStream(@NotNull String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}