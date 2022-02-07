package main.giveaway;

import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Language;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import main.startbot.Statcord;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.logging.Logger;

@AllArgsConstructor
@Service
public class ReactionsButton extends ListenerAdapter {

    public static final String PRESENT = "PRESENT";
    public static final String STOP_ONE = "STOP_ONE";
    public static final String STOP_TWO = "STOP_TWO";
    public static final String STOP_THREE = "STOP_THREE";
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
            if (GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons(event.getGuild().getIdLong()) == null) {
                return;
            }

            if (!GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons(event.getGuild().getIdLong()).equals(event.getMessageId())) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!event.getGuild().getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_SEND)) {
            return;
        }

        try {

            LOGGER.info(
                    "\nGuild id: " + event.getGuild().getId() + "" +
                            "\nUser id: " + event.getUser().getId() + "" +
                            "\nButton pressed: " + event.getButton().getId());

            long guild = event.getGuild().getIdLong();

            // TODO: Может это слишком жёстко
            new Thread(() -> {
                try {
                    System.out.println("Go to sleep");
                    Thread.sleep(10000L);
                    System.out.println("wake up");

                    if (GiveawayRegistry.getInstance().hasGift(guild) &&
                            GiveawayRegistry.getInstance().getGift(
                                            event.getGuild().getIdLong()).getListUsersHash(event.getUser().getId()) != null &&
                            participantsRepository.getParticipant(event.getGuild().getIdLong(), event.getUser().getIdLong()) == null) {

                        System.out.println(GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong()).getListUsersHash(event.getUser().getId()) != null);
                        System.out.println(participantsRepository.getParticipant(event.getUser().getIdLong(), event.getGuild().getIdLong()));
                        System.out.println("Пользователя нет в БД это странно!");

                        ActiveGiveaways activeGiveaways = activeGiveawayRepository.getActiveGiveawaysByGuildIdLong(event.getGuild().getIdLong());
                        Participants participants = new Participants();
                        participants.setUserIdLong(event.getUser().getIdLong());
                        participants.setNickName(event.getUser().getName());
                        participants.setActiveGiveaways(activeGiveaways);

                        participantsRepository.save(participants);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }).start();

            long startTime = System.currentTimeMillis();

            // TODO: Решит проблему с дубликатами
            synchronized (this) {
                if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + PRESENT)
                        && GiveawayRegistry.getInstance().hasGift(guild)
                        && GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong())
                        .getListUsersHash(event.getUser().getId()) == null) {
                    event.deferEdit().queue();

                    GiveawayRegistry.getInstance()
                            .getGift(event.getGuild().getIdLong())
                            .addUserToPoll(event.getUser());
                    Statcord.commandPost("gift", event.getUser().getId());

                    return;
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Total execution time: " + (endTime - startTime) + " ms");

            if (Objects.equals(event.getButton().getId(), event.getGuild().getId() + ":" + PRESENT)
                    && GiveawayRegistry.getInstance().hasGift(guild)
                    && GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong())
                    .getListUsersHash(event.getUser().getId()) != null) {
                event.deferEdit().queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
