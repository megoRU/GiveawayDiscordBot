package main.giveaway.slash;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.Participants;
import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.WinnersAndParticipants;
import api.megoru.ru.impl.MegoruAPIImpl;
import api.megoru.ru.io.UnsuccessfulHttpException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.giveaway.buttons.ReactionsButton;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.*;

@AllArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() == null) {
            EmbedBuilder fromGuild = new EmbedBuilder();
            fromGuild.setColor(0x00FF00);
            fromGuild.setDescription("The bot supports `/slash commands` only in guilds!");
            event.replyEmbeds(fromGuild.build()).queue();
            return;
        }

        if (event.getMember() == null) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_SEND) ||
                !event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        if (event.getName().equals("start")) {
            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(0x00FF00);
                errors.setDescription(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId()));

                event.replyEmbeds(errors.build()).queue();
            } else {
                try {
                    String title = event.getOption("title", OptionMapping::getAsString);
                    String count = event.getOption("count", OptionMapping::getAsString);
                    String time = event.getOption("duration", OptionMapping::getAsString);
                    TextChannel textChannel = event.getOption("channel", OptionMapping::getAsTextChannel);
                    Long role = event.getOption("mention", OptionMapping::getAsLong);
                    Message.Attachment image = event.getOption("image", OptionMapping::getAsAttachment);
                    String urlImage = null;

                    if (image != null && image.isImage()) {
                        urlImage = image.getUrl();
                    }

                    boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(0xFF0000);

                    if (title != null && title.length() >= MessageEmbed.TITLE_MAX_LENGTH) {
                        embedBuilder.setDescription(jsonParsers.getLocale("slash_error_256", event.getGuild().getId()) + role + "`");
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (role == null && isOnlyForSpecificRole) {
                        embedBuilder.setDescription(jsonParsers.getLocale("slash_error_only_for_this_role", event.getGuild().getId()) + role + "`");
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (role != null && role == event.getGuild().getIdLong() && isOnlyForSpecificRole) {
                        embedBuilder.setDescription(jsonParsers.getLocale("slash_error_role_can_not_be_everyone", event.getGuild().getId()));
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    GiveawayRegistry.getInstance().putGift(
                            event.getGuild().getIdLong(),
                            new Gift(event.getGuild().getIdLong(),
                                    textChannel == null ? event.getTextChannel().getIdLong() : textChannel.getIdLong(),
                                    event.getUser().getIdLong(),
                                    activeGiveawayRepository,
                                    participantsRepository));

                    if (!event.getGuild().getSelfMember()
                            .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_SEND)
                            || !event.getGuild().getSelfMember()
                            .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_EMBED_LINKS)) {
                        return;
                    }

                    GiveawayRegistry.getInstance()
                            .getGift(event.getGuild().getIdLong()).startGift(event,
                                    event.getGuild(),
                                    textChannel == null ? event.getTextChannel() : textChannel,
                                    title,
                                    count,
                                    time,
                                    role,
                                    isOnlyForSpecificRole,
                                    urlImage,
                                    event.getUser().getIdLong());

                    //Мы не будет очищать это, всё равно рано или поздно будет перезаписываться или даже не будет в случае Exception
                    GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(event.getGuild().getIdLong(), event.getUser().getId());

                    //Если время будет неверным. Сработает try catch
                } catch (Exception e) {
                    e.printStackTrace();

                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(0x00FF00);
                    errors.setDescription(jsonParsers.getLocale("slash_Errors", event.getGuild().getId()));

                    event.replyEmbeds(errors.build()).queue();
                    GiveawayRegistry.getInstance().removeGift(event.getGuild().getIdLong());
                    activeGiveawayRepository.deleteActiveGiveaways(event.getGuild().getIdLong());
                }
            }
            return;
        }

        if (event.getName().equals("stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                EmbedBuilder notHas = new EmbedBuilder();
                notHas.setColor(0x00FF00);
                notHas.setDescription(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId()));

                event.replyEmbeds(notHas.build()).queue();
                return;
            }

            if (!event.getMember().hasPermission(event.getTextChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {

                EmbedBuilder gift = new EmbedBuilder();
                gift.setColor(0x00FF00);
                gift.setDescription(jsonParsers.getLocale("message_gift_Not_Admin", event.getGuild().getId()));

                event.replyEmbeds(gift.build()).setEphemeral(true).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                EmbedBuilder stop = new EmbedBuilder();
                stop.setColor(0x00FF00);
                stop.setDescription(jsonParsers.getLocale("slash_Stop", event.getGuild().getId()));

                event.replyEmbeds(stop.build()).queue();

                GiveawayRegistry.getInstance()
                        .getGift(event.getGuild().getIdLong())
                        .stopGift(event.getGuild().getIdLong(),
                                GiveawayRegistry.getInstance().getCountWinners(event.getGuild().getIdLong()) == null
                                        ? 1
                                        : Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners(event.getGuild().getIdLong()))
                        );
                return;
            }

            if (!event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")) {
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setDescription(jsonParsers.getLocale("slash_Errors", event.getGuild().getId()));

                event.replyEmbeds(errors.build()).queue();
                return;
            }

            EmbedBuilder stop = new EmbedBuilder();

            Long count = event.getOption("count", OptionMapping::getAsLong);
            boolean isHasErrors = false;

            if (count == null) return;

            Gift gift = GiveawayRegistry.getInstance().getGift(event.getGuild().getIdLong());

            if (gift.getListUsersSize() <= count) {
                isHasErrors = true;
            }

            if (!isHasErrors) {
                stop.setColor(Color.GREEN);
                stop.setDescription(jsonParsers.getLocale("slash_Stop", event.getGuild().getId()));
                event.replyEmbeds(stop.build()).queue();
            } else {
                stop.setColor(Color.RED);
                stop.setDescription(jsonParsers.getLocale("slash_Stop_Errors", event.getGuild().getId()));
                event.replyEmbeds(stop.build()).queue();
            }

            GiveawayRegistry.getInstance()
                    .getGift(event.getGuild().getIdLong())
                    .stopGift(event.getGuild().getIdLong(), Integer.parseInt(event.getOptions().get(0).getAsString()));
            return;
        }

        if (event.getName().equals("help")) {

            String guildIdLong = event.getGuild().getId();

            EmbedBuilder info = new EmbedBuilder();
            info.setColor(0xa224db);
            info.setTitle("Giveaway");
            info.addField("Slash Commands", "`/language`, `/start`, `/stop`, `/list`" +
                    "\n`/reroll`, `/participants`, `/patreon`", false);

            info.addField(jsonParsers.getLocale("messages_events_Links", guildIdLong),
                    jsonParsers.getLocale("messages_events_Site", guildIdLong) +
                            jsonParsers.getLocale("messages_events_Add_Me_To_Other_Guilds", guildIdLong), false);

            List<Button> buttons = new ArrayList<>();

            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

            if (BotStartConfig.getMapLanguages().get(guildIdLong) != null) {

                if (BotStartConfig.getMapLanguages().get(guildIdLong).equals("eng")) {

                    buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                    "Сменить язык ")
                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                } else {
                    buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                    "Change language ")
                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                }
            } else {
                buttons.add(Button.secondary(guildIdLong + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                "Сменить язык ")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            }

            event.replyEmbeds(info.build()).setEphemeral(true).addActionRow(buttons).queue();
            return;
        }

        //0 - bot
        if (event.getName().equals("language")) {

            if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {

                EmbedBuilder notAdmin = new EmbedBuilder();
                notAdmin.setColor(Color.GREEN);
                notAdmin.setDescription(jsonParsers.getLocale("language_change_Not_Admin", event.getGuild().getId())
                        .replaceAll("\\{0}", event.getOptions().get(0).getAsString()));

                event.replyEmbeds(notAdmin.build()).setEphemeral(true).queue();
                return;
            }

            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), event.getOptions().get(0).getAsString());

            EmbedBuilder button = new EmbedBuilder();
            button.setColor(Color.GREEN);
            button.setDescription(jsonParsers.getLocale("button_Language", event.getGuild().getId())
                    .replaceAll("\\{0}", event.getOptions().get(0).getAsString().equals("rus")
                            ? "Русский"
                            : "English"));

            event.replyEmbeds(button.build()).setEphemeral(true).queue();

            Language language = new Language();
            language.setServerId(event.getGuild().getId());
            language.setLanguage(event.getOptions().get(0).getAsString());
            languageRepository.save(language);
            return;
        }

        if (event.getName().equals("list")) {

            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {

                StringBuilder stringBuilder = new StringBuilder();
                List<String> participantsList = new ArrayList<>(GiveawayRegistry.getInstance()
                        .getGift(event.getGuild().getIdLong())
                        .getListUsersHash().values());

                if (participantsList.isEmpty()) {
                    EmbedBuilder list = new EmbedBuilder();
                    list.setColor(Color.GREEN);
                    list.setDescription(jsonParsers.getLocale("slash_list_users_empty", event.getGuild().getId()));
                    event.replyEmbeds(list.build()).setEphemeral(true).queue();
                    return;
                }

                for (int i = 0; i < participantsList.size(); i++) {
                    if (stringBuilder.length() < 4000) {
                        stringBuilder.append(stringBuilder.length() == 0 ? "<@" : ", <@").append(participantsList.get(i)).append(">");
                    } else {
                        stringBuilder.append(" and others...");
                        break;
                    }
                }

                EmbedBuilder list = new EmbedBuilder();
                list.setColor(Color.GREEN);
                list.setTitle(jsonParsers.getLocale("slash_list_users", event.getGuild().getId()));
                list.setDescription(stringBuilder);

                event.replyEmbeds(list.build()).queue();
            } else {
                EmbedBuilder noGiveaway = new EmbedBuilder();
                noGiveaway.setColor(Color.orange);
                noGiveaway.setDescription(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId()));
                event.replyEmbeds(noGiveaway.build()).setEphemeral(true).queue();
            }
            return;
        }

        if (event.getName().equals("reroll")) {
            event.deferReply().queue();

            if (!event.getOption("id").getAsString().matches("\\d+")) {
                event.getHook().sendMessage("ID is not Number!").setEphemeral(true).queue();
                return;
            }

            Long id = event.getOption("id", OptionMapping::getAsLong);

            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                EmbedBuilder giveaway = new EmbedBuilder();
                giveaway.setColor(Color.orange);
                giveaway.setDescription(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId()));
                event.getHook().sendMessageEmbeds(giveaway.build()).setEphemeral(true).queue();
                return;
            }


            MegoruAPI api = new MegoruAPIImpl(System.getenv("BASE64_PASSWORD"));
            Participants[] listUsers;

            try {
                listUsers = api.getListUsers(event.getUser().getId(), String.valueOf(id));

                Winners winners = new Winners(1, 0, listUsers.length - 1);

                WinnersAndParticipants winnersAndParticipants = new WinnersAndParticipants();
                winnersAndParticipants.setUpdate(false);
                winnersAndParticipants.setWinners(winners);
                winnersAndParticipants.setUserList(List.of(listUsers));

                String[] strings = api.setWinners(winnersAndParticipants);

                final Set<String> uniqueWinners = new LinkedHashSet<>();


                if (strings == null) throw new Exception("API not work, or connection refused");

                for (int i = 0; i < strings.length; i++) {
                    uniqueWinners.add("<@" + listUsers[Integer.parseInt(strings[i])].getUserIdLong() + ">");
                }

                EmbedBuilder winner = new EmbedBuilder();
                winner.setColor(Color.GREEN);
                winner.setDescription(jsonParsers.getLocale("gift_congratulations_reroll",
                        event.getGuild().getId()).replaceAll("\\{0}",
                        Arrays.toString(uniqueWinners.toArray())
                        .replaceAll("\\[", "")
                        .replaceAll("]", "")));


                event.getHook().sendMessageEmbeds(winner.build()).queue();
            } catch (UnsuccessfulHttpException exception) {
                if (exception.getCode() == 404) {
                    event.getHook().sendMessage(exception.getMessage()).setEphemeral(true).queue();
                } else {
                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(Color.RED);
                    errors.setTitle("Errors with API");
                    errors.setDescription("Repeat later. Or write to us about it.");

                    List<net.dv8tion.jda.api.interactions.components.buttons.Button> buttons = new ArrayList<>();
                    buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

                    event.getHook().sendMessageEmbeds(errors.build()).addActionRow(buttons).queue();
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (event.getName().equals("participants")) {
            try {
                event.deferReply().setEphemeral(true).queue();
                String id = event.getOption("id", OptionMapping::getAsString);

                File file = new File("participants.json");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                MegoruAPI api = new MegoruAPIImpl(System.getenv("BASE64_PASSWORD"));
                Participants[] listUsers;
                try {
                    listUsers = api.getListUsers(event.getUser().getId(), id);
                } catch (UnsuccessfulHttpException exception) {
                    if (exception.getCode() == 404) {
                        event.getHook().sendMessage(exception.getMessage()).setEphemeral(true).queue();
                    } else {
                        EmbedBuilder errors = new EmbedBuilder();
                        errors.setColor(Color.RED);
                        errors.setTitle("Errors with API");
                        errors.setDescription("Repeat later. Or write to us about it.");

                        List<net.dv8tion.jda.api.interactions.components.buttons.Button> buttons = new ArrayList<>();
                        buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

                        event.getHook().sendMessageEmbeds(errors.build()).addActionRow(buttons).queue();
                    }
                    return;
                }

                String json = gson.toJson(listUsers);

                // Создание объекта FileWriter
                FileWriter writer = new FileWriter(file);

                // Запись содержимого в файл
                writer.write(json);
                writer.flush();
                writer.close();

                event.getHook().sendFile(file).setEphemeral(true).queue();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (event.getName().equals("patreon")) {
            EmbedBuilder patreon = new EmbedBuilder();
            patreon.setColor(Color.YELLOW);
            patreon.setTitle("Patreon", "https://www.patreon.com/ghbots");
            patreon.setDescription("If you want to support the work of our bots." +
                    "\nYou can do it here click: [here](https://www.patreon.com/ghbots)");
            event.replyEmbeds(patreon.build()).setEphemeral(true).queue();
        }
    }
}