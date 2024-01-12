package main.core.events;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayEmbedUtils;
import main.giveaway.GiveawayMessageHandler;
import main.giveaway.GiveawayRegistry;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.repository.ActiveGiveawayRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class ChangeCommand {

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayMessageHandler giveawayMessageHandler;

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public ChangeCommand(ActiveGiveawayRepository activeGiveawayRepository,
                         GiveawayMessageHandler giveawayMessageHandler) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.giveawayMessageHandler = giveawayMessageHandler;
    }

    public void change(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;
        var guildId = event.getGuild().getIdLong();

        Giveaway giveaway = GiveawayRegistry.getInstance().getGiveaway(guildId);
        if (giveaway == null) {
            String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", guildId);
            event.reply(slashStopNoHas).setEphemeral(true).queue();
            return;
        }
        String time = event.getOption("duration", OptionMapping::getAsString);
        if (time != null) {
            if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
                if (GiveawayUtils.timeHandler(event, guildId, time)) return;

                long channelId = giveaway.getTextChannelId();
                long messageId = giveaway.getMessageId();

                Timestamp timestamp = giveaway.updateTime(time);

                String changeDuration = jsonParsers.getLocale("change_duration", guildId);
                event.reply(changeDuration).setEphemeral(true).queue();

                activeGiveawayRepository.updateGiveawayTime(guildId, timestamp);
                EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayLayout(guildId);

                giveawayMessageHandler.editMessage(embedBuilder.build(), guildId, channelId, messageId);
            }
        }
    }
}