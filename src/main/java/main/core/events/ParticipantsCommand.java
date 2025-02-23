package main.core.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.jsonparser.JSONParsers;
import main.model.entity.ListUsers;
import main.model.repository.ListUsersRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ParticipantsCommand {

    private static final JSONParsers jsonParsers = new JSONParsers();
    private final ListUsersRepository listUsersRepository;

    public void participants(@NotNull SlashCommandInteractionEvent event) {
        var userIdLong = event.getUser().getIdLong();
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();

        event.deferReply().setEphemeral(true).queue();
        String id = event.getOption("giveaway-id", OptionMapping::getAsString);

        if (id != null) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayIdAndCreatedUserId(Long.parseLong(id), userIdLong);
            if (listUsers.isEmpty()) {
                String noAccessReroll = jsonParsers.getLocale("no_access_reroll", guildId);
                event.getHook().sendMessage(noAccessReroll).setEphemeral(true).queue();
                return;
            }
            String json = gson.toJson(listUsers);
            InputStream inputStream = getInputStream(json);
            FileUpload fileUpload = FileUpload.fromData(inputStream, "participants.json");
            event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
        } else {
            event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
        }
    }

    private InputStream getInputStream(@NotNull String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}