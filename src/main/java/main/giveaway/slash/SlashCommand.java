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
import main.giveaway.ChecksClass;
import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.giveaway.buttons.ReactionsButton;
import main.jsonparser.JSONParsers;
import main.model.entity.Language;
import main.model.entity.Notification;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.NotificationRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
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
    private final NotificationRepository notificationRepository;

    private static final String TIME_REGEX = "^(\\d{1,2}h|\\d{1,2}m|\\d{1,2}d|\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2})$";

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;
        if (event.getMember() == null) return;
        if (event.getGuild() == null) return;
        if (event.getChannelType().isThread()) {
            String startInThread = jsonParsers.getLocale("start_in_thread", event.getGuild().getId());
            event.reply(startInThread).queue();
            return;
        }

        if (event.getName().equals("check-bot-permission")) {
            GuildChannelUnion textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);
            GuildChannel guildChannel = textChannel != null ? textChannel : event.getGuildChannel().asTextChannel();

            boolean canSendGiveaway = ChecksClass.canSendGiveaway(guildChannel, event);

            if (canSendGiveaway) {
                String giftPermissions = String.format(jsonParsers.getLocale("gift_permissions", event.getGuild().getId()), guildChannel.getId());

                event.reply(giftPermissions).queue();
            }
            return;
        }

        if (event.getName().equals("start")) {
            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", event.getGuild().getId());

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(0x00FF00);
                errors.setDescription(messageGiftNeedStopGiveaway);

                event.replyEmbeds(errors.build()).queue();
            } else {
                GuildChannelUnion textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);

                if (textChannel != null && !textChannel.getType().equals(ChannelType.TEXT)) {
                    String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", event.getGuild().getId());
                    event.reply(startInNotTextChannels).queue();
                    return;
                } else if (textChannel != null && textChannel.getType().equals(ChannelType.TEXT)) {
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannel.asTextChannel(), event);
                    if (!canSendGiveaway) return;
                } else if (textChannel == null) {
                    boolean canSendGiveaway = ChecksClass.canSendGiveaway(event.getGuildChannel().asTextChannel(), event);
                    if (!canSendGiveaway) return;
                }

                try {
                    String title = event.getOption("title", OptionMapping::getAsString);

                    int count = 1;
                    String countString = event.getOption("count", OptionMapping::getAsString);
                    if (countString != null) {
                        count = Integer.parseInt(countString);
                    }
                    String time = event.getOption("duration", OptionMapping::getAsString);
                    Long role = event.getOption("mention", OptionMapping::getAsLong);
                    Message.Attachment image = event.getOption("image", OptionMapping::getAsAttachment);
                    String urlImage = null;

                    if (image != null && image.isImage()) {
                        urlImage = image.getUrl();
                    }

                    boolean isOnlyForSpecificRole = Objects.equals(event.getOption("role", OptionMapping::getAsString), "yes");

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setColor(0xFF0000);

                    if (time != null && !time.matches(TIME_REGEX)) {
                        String startExamples = jsonParsers.getLocale("start_examples", event.getGuild().getId());
                        String startWrongTime = String.format(jsonParsers.getLocale("start_wrong_time", event.getGuild().getId()),
                                time,
                                startExamples);

                        embedBuilder.setDescription(startWrongTime);
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (title != null && title.length() >= MessageEmbed.TITLE_MAX_LENGTH) {
                        String slashError256 = jsonParsers.getLocale("slash_error_256", event.getGuild().getId());
                        embedBuilder.setDescription(slashError256);
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (role == null && isOnlyForSpecificRole) {
                        String slashErrorOnlyForThisRole = String.format(jsonParsers.getLocale("slash_error_only_for_this_role", event.getGuild().getId()), role);
                        embedBuilder.setDescription(slashErrorOnlyForThisRole);

                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    if (role != null && role == event.getGuild().getIdLong() && isOnlyForSpecificRole) {
                        String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", event.getGuild().getId());
                        embedBuilder.setDescription(slashErrorRoleCanNotBeEveryone);
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    GiveawayRegistry.getInstance().putGift(
                            event.getGuild().getIdLong(),
                            new Gift(event.getGuild().getIdLong(),
                                    textChannel == null ? event.getChannel().getIdLong() : textChannel.getIdLong(),
                                    event.getUser().getIdLong(),
                                    activeGiveawayRepository,
                                    participantsRepository));

                    GiveawayRegistry.getInstance()
                            .getGift(event.getGuild().getIdLong())
                            .startGift(event,
                                    event.getGuild(),
                                    textChannel == null ? event.getChannel().asTextChannel() : textChannel.asTextChannel(),
                                    title,
                                    count,
                                    time,
                                    role,
                                    isOnlyForSpecificRole,
                                    urlImage,
                                    event.getUser().getIdLong());

                    //Мы не будет очищать это, всё равно рано или поздно будет перезаписываться или даже не будет в случае Exception
                    GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(event.getGuild().getIdLong(), event.getUser().getIdLong());

                    //Если время будет неверным. Сработает try catch
                } catch (Exception e) {
                    e.printStackTrace();
                    String slashErrors = jsonParsers.getLocale("slash_errors", event.getGuild().getId());
                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(0x00FF00);
                    errors.setDescription(slashErrors);

                    event.replyEmbeds(errors.build()).queue();
                    GiveawayRegistry.getInstance().removeGuildFromGiveaway(event.getGuild().getIdLong());
                    activeGiveawayRepository.deleteActiveGiveaways(event.getGuild().getIdLong());
                }
            }
            return;
        }

        if (event.getName().equals("stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", event.getGuild().getId());

                EmbedBuilder notHas = new EmbedBuilder();
                notHas.setColor(0x00FF00);
                notHas.setDescription(slashStopNoHas);

                event.replyEmbeds(notHas.build()).queue();
                return;
            }

            if (!event.getMember().hasPermission(event.getGuildChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                String messageGiftNotAdmin = jsonParsers.getLocale("message_gift_not_admin", event.getGuild().getId());

                EmbedBuilder gift = new EmbedBuilder();
                gift.setColor(0x00FF00);
                gift.setDescription(messageGiftNotAdmin);

                event.replyEmbeds(gift.build()).setEphemeral(true).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                String slashStop = jsonParsers.getLocale("slash_stop", event.getGuild().getId());

                EmbedBuilder stop = new EmbedBuilder();
                stop.setColor(0x00FF00);
                stop.setDescription(slashStop);

                event.replyEmbeds(stop.build()).queue();

                int count = GiveawayRegistry.getInstance().getCountWinners(event.getGuild().getIdLong());

                GiveawayRegistry.getInstance()
                        .getGift(event.getGuild().getIdLong())
                        .stopGift(event.getGuild().getIdLong(), count);
                return;
            }

            if (!event.getOptions().get(0).getAsString().matches("\\d{1,2}")) {
                String slashErrors = jsonParsers.getLocale("slash_errors", event.getGuild().getId());

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setDescription(slashErrors);

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
                String slashStop = jsonParsers.getLocale("slash_stop", event.getGuild().getId());

                stop.setColor(Color.GREEN);
                stop.setDescription(slashStop);
                event.replyEmbeds(stop.build()).queue();
            } else {
                String slashStopErrors = jsonParsers.getLocale("slash_stop_errors", event.getGuild().getId());

                stop.setColor(Color.RED);
                stop.setDescription(slashStopErrors);
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

            String messagesEventsLinks = jsonParsers.getLocale("messages_events_links", guildIdLong);
            String messagesEventsSite = jsonParsers.getLocale("messages_events_site", guildIdLong);
            String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_add_me_to_other_guilds", guildIdLong);

            info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds, false);

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

                String languageChangeNotAdmin = jsonParsers.getLocale("language_change_not_admin", event.getGuild().getId());

                EmbedBuilder notAdmin = new EmbedBuilder();
                notAdmin.setColor(Color.GREEN);
                notAdmin.setDescription(languageChangeNotAdmin);

                event.replyEmbeds(notAdmin.build()).setEphemeral(true).queue();
                return;
            }

            BotStartConfig.getMapLanguages().put(event.getGuild().getId(), event.getOptions().get(0).getAsString());

            String lang = event.getOptions().get(0).getAsString().equals("rus") ? "Русский" : "English";
            String buttonLanguage = String.format(jsonParsers.getLocale("button_language", event.getGuild().getId()), lang);

            EmbedBuilder button = new EmbedBuilder();
            button.setColor(Color.GREEN);
            button.setDescription(buttonLanguage);

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
                    String slashListUsersEmpty = jsonParsers.getLocale("slash_list_users_empty", event.getGuild().getId());

                    EmbedBuilder list = new EmbedBuilder();
                    list.setColor(Color.GREEN);
                    list.setDescription(slashListUsersEmpty);
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

                String slashListUsers = jsonParsers.getLocale("slash_list_users", event.getGuild().getId());

                EmbedBuilder list = new EmbedBuilder();
                list.setColor(Color.GREEN);
                list.setTitle(slashListUsers);
                list.setDescription(stringBuilder);

                event.replyEmbeds(list.build()).queue();
            } else {
                String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", event.getGuild().getId());

                EmbedBuilder noGiveaway = new EmbedBuilder();
                noGiveaway.setColor(Color.orange);
                noGiveaway.setDescription(slashStopNoHas);
                event.replyEmbeds(noGiveaway.build()).setEphemeral(true).queue();
            }
            return;
        }

        if (event.getName().equals("notifications")) {
            event.deferReply().queue();

            Notification.NotificationStatus notificationStatus;
            String userId = event.getUser().getId();

            EmbedBuilder giveaway = new EmbedBuilder();
            giveaway.setColor(Color.GREEN);

            if (event.getOptions().get(0).getAsString().equals("enable")) {
                String buttonNotificationAccept = jsonParsers.getLocale("button_notification_accept", event.getGuild().getId());
                notificationStatus = Notification.NotificationStatus.ACCEPT;
                giveaway.setDescription(buttonNotificationAccept);
            } else {
                String buttonNotificationDeny = jsonParsers.getLocale("button_notification_deny", event.getGuild().getId());
                notificationStatus = Notification.NotificationStatus.DENY;
                giveaway.setDescription(buttonNotificationDeny);
            }

            BotStartConfig.getMapNotifications().put(userId, notificationStatus);

            Notification notification = new Notification();
            notification.setUserIdLong(userId);
            notification.setNotificationStatus(notificationStatus);

            event.getHook().sendMessageEmbeds(giveaway.build())
                    .setEphemeral(true)
                    .queue();

            notificationRepository.save(notification);
            return;
        }


        if (event.getName().equals("reroll")) {
            event.deferReply().queue();
            OptionMapping option = event.getOption("id");

            if (option != null) {

                if (!option.getAsString().matches("\\d+")) {
                    event.getHook().sendMessage("ID is not Number!").setEphemeral(true).queue();
                    return;
                }

                Long id = event.getOption("id", OptionMapping::getAsLong);

                if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                    String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", event.getGuild().getId());

                    EmbedBuilder giveaway = new EmbedBuilder();
                    giveaway.setColor(Color.orange);
                    giveaway.setDescription(messageGiftNeedStopGiveaway);
                    event.getHook().sendMessageEmbeds(giveaway.build()).setEphemeral(true).queue();
                    return;
                }


                MegoruAPI api = new MegoruAPIImpl(System.getenv("BASE64_PASSWORD"));

                try {
                    Participants[] listUsers = api.getListUsers(event.getUser().getId(), String.valueOf(id));

                    Winners winners = new Winners(1, 0, listUsers.length - 1);

                    WinnersAndParticipants winnersAndParticipants = new WinnersAndParticipants();
                    winnersAndParticipants.setUpdate(false);
                    winnersAndParticipants.setWinners(winners);
                    winnersAndParticipants.setUserList(List.of(listUsers));

                    String[] strings = api.setWinners(winnersAndParticipants);

                    final Set<String> uniqueWinners = new LinkedHashSet<>();

                    for (int i = 0; i < strings.length; i++) {
                        uniqueWinners.add("<@" + listUsers[Integer.parseInt(strings[i])].getUserIdLong() + ">");
                    }

                    String winnerList = Arrays.toString(uniqueWinners.toArray())
                            .replaceAll("\\[", "")
                            .replaceAll("]", "");
                    String giftCongratulationsReroll = String.format(jsonParsers.getLocale("gift_congratulations_reroll",
                            event.getGuild().getId()), winnerList);

                    EmbedBuilder winner = new EmbedBuilder();
                    winner.setColor(Color.GREEN);
                    winner.setDescription(giftCongratulationsReroll);


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
            } else {
                event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
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

                        List<Button> buttons = new ArrayList<>();
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

                FileUpload fileUpload = FileUpload.fromData(file);

                event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
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