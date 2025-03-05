package main.core.events;

import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Scheduling;
import main.model.repository.SchedulingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import static main.giveaway.GiveawayUtils.timeProcessor;

@Service
public class SchedulingCommand {

    private final SchedulingRepository schedulingRepository;

    private static final JSONParsers jsonParsers = new JSONParsers();

    @Autowired
    public SchedulingCommand(SchedulingRepository schedulingRepository) {
        this.schedulingRepository = schedulingRepository;
    }

    public void scheduling(@NotNull SlashCommandInteractionEvent event) {
        var guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
        var userId = event.getUser().getIdLong();
        var role = event.getOption("mention", OptionMapping::getAsLong);
        int winners = Optional.ofNullable(event.getOption("winners", OptionMapping::getAsInt)).orElse(1);
        var title = Optional.ofNullable(event.getOption("title", OptionMapping::getAsString)).orElse("Giveaway");
        var textChannel = event.getOption("text-channel", OptionMapping::getAsChannel);
        var image = event.getOption("image", OptionMapping::getAsAttachment);
        var urlImage = image != null ? image.getUrl() : null;
        int minParticipants = Optional.ofNullable(event.getOption("min-participants", OptionMapping::getAsInt)).orElse(1);
        var startTime = Objects.requireNonNull(event.getOption("start-time", OptionMapping::getAsString));
        var endTime = event.getOption("end-time", OptionMapping::getAsString);
        boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

        if (textChannel == null && event.getChannel() instanceof GuildChannelUnion) {
            textChannel = (GuildChannelUnion) event.getChannel();
        }

        boolean checkPermissions = GiveawayUtils.checkPermissions(textChannel, event.getGuild().getSelfMember());

        if (!checkPermissions) {
            String botPermissionsDeny = jsonParsers.getLocale("bot_permissions_deny", guildId);
            event.reply(botPermissionsDeny).queue();
            return; //Сообщение уже отправлено
        }

        //Обработать уведомление
        event.deferReply().setEphemeral(true).queue();

        if (!GiveawayUtils.isISOTimeCorrect(startTime)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.getHook().sendMessage(wrongDate).queue();
            return;
        }

        if (endTime != null && !GiveawayUtils.isISOTimeCorrect(endTime)) {
            String wrongDate = jsonParsers.getLocale("wrong_date", guildId);
            event.getHook().sendMessage(wrongDate).queue();
            return;
        }

        if (textChannel instanceof NewsChannel || textChannel instanceof TextChannel) {
            if (role == null && isOnlyForSpecificRole) {
                String slashErrorOnlyForThisRole = jsonParsers.getLocale("slash_error_only_for_this_role", guildId);
                event.getHook().sendMessage(slashErrorOnlyForThisRole).setEphemeral(true).queue();
                return;
            } else if (role != null && role == guildId && isOnlyForSpecificRole) {
                String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", guildId);
                event.getHook().sendMessage(slashErrorRoleCanNotBeEveryone).setEphemeral(true).queue();
                return;
            }
            String salt = GiveawayUtils.getSalt(20);

            Scheduling scheduling = new Scheduling();
            scheduling.setIdSalt(salt);
            scheduling.setGuildId(guildId);
            scheduling.setChannelId(textChannel.getIdLong());
            scheduling.setCountWinners(winners);
            scheduling.setDateCreateGiveaway(timeProcessor(startTime));
            scheduling.setDateEnd(timeProcessor(endTime) == null ? null : timeProcessor(endTime));
            scheduling.setTitle(title);
            scheduling.setRoleId(role);
            scheduling.setIsForSpecificRole(isOnlyForSpecificRole);
            scheduling.setCreatedUserId(userId);
            scheduling.setUrlImage(urlImage);
            scheduling.setMinParticipants(minParticipants);

            schedulingRepository.save(scheduling);

            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            instance.putScheduling(salt, scheduling);

            String scheduleEnd = jsonParsers.getLocale("schedule_end", guildId);
            long timeStart = Objects.requireNonNull(timeProcessor(startTime)).getTime() / 1000;
            if (endTime != null) {
                long timeEnd = Objects.requireNonNull(timeProcessor(endTime)).getTime() / 1000;
                if (timeEnd != 0) {
                    scheduleEnd = String.format("<t:%s:R> (<t:%s:f>)", timeEnd, timeEnd);
                }
            }

            String scheduleRole = jsonParsers.getLocale("schedule_end", guildId);

            String scheduleStart = String.format(jsonParsers.getLocale("schedule_start", guildId),
                    timeStart,
                    timeStart,
                    scheduleEnd,
                    role == null ? scheduleRole : "<@&" + role + ">",
                    textChannel.getId(),
                    salt);

            EmbedBuilder start = new EmbedBuilder();
            start.setColor(GiveawayUtils.getUserColor(guildId));
            start.setDescription(scheduleStart);

            event.getHook().sendMessageEmbeds(start.build()).setEphemeral(true).queue();
        } else {
            String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", guildId);
            event.getHook().sendMessage(startInNotTextChannels).queue();
        }
    }
}