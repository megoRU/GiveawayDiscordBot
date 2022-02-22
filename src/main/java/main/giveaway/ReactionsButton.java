package main.giveaway;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.messagesevents.MessageInfoHelp;
import main.messagesevents.SenderMessage;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Language;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import main.startbot.Statcord;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Objects;
import java.util.logging.Logger;

@AllArgsConstructor
@Service
public class ReactionsButton extends ListenerAdapter implements SenderMessage {

    public static final String PRESENT = "PRESENT";
    public static final String BUTTON_EXAMPLES = "BUTTON_EXAMPLES";
    public static final String BUTTON_HELP = "BUTTON_HELP";
    public static final String CHANGE_LANGUAGE = "CHANGE_LANGUAGE";
    private final static Logger LOGGER = Logger.getLogger(ReactionsButton.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final ParticipantsRepository participantsRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton() == null) return;

        if (event.getGuild() == null || event.getMember() == null) return;

        if (event.getUser().isBot()) return;

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_EXAMPLES)) {

            event.deferEdit().queue();
            event.getChannel().sendMessage(jsonParsers
                            .getLocale("message_gift_Not_Correct_For_Button", event.getGuild().getId())
                            .replaceAll("\\{0}",
                                    BotStartConfig.getMapPrefix().get(event.getGuild().getId())
                                            == null
                                            ? "!"
                                            : BotStartConfig.getMapPrefix().get(event.getGuild().getId())))
                    .setActionRow(Button.success(event.getGuild().getId() + ":" + ReactionsButton.BUTTON_HELP,
                            jsonParsers.getLocale("button_Help", event.getGuild().getId())))
                    .queue();
            return;
        }

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + BUTTON_HELP)) {

            event.deferEdit().queue();
            MessageInfoHelp messageInfoHelp = new MessageInfoHelp();
            messageInfoHelp.buildMessage(
                    BotStartConfig.getMapPrefix().get(event.getGuild().getId()) == null ? "!" : BotStartConfig.getMapPrefix().get(event.getGuild().getId()),
                    event.getTextChannel(),
                    event.getUser().getAvatarUrl(),
                    event.getGuild().getId(),
                    event.getUser().getName(),
                    null
            );
            return;
        }

        if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + CHANGE_LANGUAGE)) {
            event.deferEdit().queue();
            String buttonName = event.getButton().getEmoji().getName().contains("\uD83C\uDDF7\uD83C\uDDFA") ? "rus" : "eng";

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage(buttonName);
            languageRepository.save(language);

            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), buttonName);

            event.getHook().sendMessage(jsonParsers
                            .getLocale("button_Language", event.getGuild().getId())
                            .replaceAll("\\{0}", buttonName))
                    .setEphemeral(true).queue();
            return;
        }

        /**
         * Если нет {@link Gift} активного, то делаем {@return}
         */
        try {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) return;

            if (GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons(event.getGuild().getIdLong()) == null) {
                return;
            }

            if (!GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons(event.getGuild().getIdLong()).equals(event.getMessageId())) {
                return;
            }

            if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)) return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            LOGGER.info(
                    "\nGuild id: " + event.getGuild().getId() + "" +
                            "\nUser id: " + event.getUser().getId() + "" +
                            "\nButton pressed: " + event.getButton().getId());

            long guild = event.getGuild().getIdLong();

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + PRESENT)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong())
                    .getListUsersHash(event.getUser().getId()) == null) {

                event.deferEdit().queue();

                if (GiveawayRegistry.getInstance().getIsForSpecificRole(guild) != null
                        && GiveawayRegistry.getInstance().getIsForSpecificRole(guild)
                        && !event.getMember().getRoles().toString().contains(GiveawayRegistry.getInstance().getRoleId(guild).toString())) {

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(Color.RED);
                    embedBuilder.setDescription(jsonParsers.getLocale("button_giveaway_not_access", event.getGuild().getId()));

                    event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                    return;
                }

                GiveawayRegistry.getInstance()
                        .getGift(event.getGuild().getIdLong())
                        .addUserToPoll(event.getUser());
                Statcord.commandPost("gift", event.getUser().getId());
                return;
            }

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + PRESENT)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong())
                    .getListUsersHash().containsKey(event.getUser().getId())) {

                long startTime = System.currentTimeMillis();

                Participants participants = participantsRepository.getParticipant(guild, event.getIdLong());

                long endTime = System.currentTimeMillis();
                System.out.println("Total execution time: " + (endTime - startTime) + " ms");

                event.deferEdit().queue();

                //TODO: Надо бы это фиксить если такое случиться
                if (participants != null) {

                    event.getHook().sendMessage("""
                                    It looks like we haven't registered you yet.\s
                                    `Try again after 5 seconds`. If it happens again, write to us in support.\s
                                    The link is available in the bot profile.
                                    """)
                            .setEphemeral(true).queue();
                } else {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(Color.GREEN);
                    embedBuilder.setDescription(jsonParsers.getLocale("button_dont_worry", event.getGuild().getId()));
                    event.getHook().sendMessageEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
