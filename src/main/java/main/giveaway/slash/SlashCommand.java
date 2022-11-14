package main.giveaway.slash;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import main.config.BotStartConfig;
import main.giveaway.ChecksClass;
import main.giveaway.Exceptions;
import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.giveaway.buttons.ReactionsButton;
import main.giveaway.impl.GiftHelper;
import main.jsonparser.JSONParsers;
import main.messagesevents.EditMessage;
import main.model.entity.Language;
import main.model.entity.ListUsers;
import main.model.entity.Notification;
import main.model.entity.Participants;
import main.model.repository.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class SlashCommand extends ListenerAdapter {

    private final static JSONParsers jsonParsers = new JSONParsers();
    private final LanguageRepository languageRepository;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationRepository notificationRepository;
    private final ListUsersRepository listUsersRepository;
    private final static MegoruAPI api = new MegoruAPI.Builder().build();

    /*
    Все доступные варианты:
    3h
    3ч
    2d
    2д
    30s
    30с
    30s 30m 1h
    30с 30м 1ч
    2021.11.16 16:00
    */
    private static final String TIME_REGEX = "(\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2})|(\\d{1,2}[smhdсмдч]| )+";
    public static final String ISO_TIME_REGEX = "^\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2}$"; //2021.11.16 16:00

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() != null && event.getChannelType().isThread()) {
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

        if (event.getName().equals("notifications")) {
            event.deferReply().queue();

            Notification.NotificationStatus notificationStatus;
            String userId = event.getUser().getId();

            EmbedBuilder giveaway = new EmbedBuilder();
            giveaway.setColor(Color.GREEN);

            Guild guild = event.getGuild();
            if (event.getOptions().get(0).getAsString().equals("enable")) {
                String buttonNotificationAccept = jsonParsers.getLocale("button_notification_accept", guild == null ? "0" : guild.getId());
                notificationStatus = Notification.NotificationStatus.ACCEPT;
                giveaway.setDescription(buttonNotificationAccept);
            } else {
                String buttonNotificationDeny = jsonParsers.getLocale("button_notification_deny", guild == null ? "0" : guild.getId());
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

        long guildIdLong = event.getGuild().getIdLong();
        if (event.getName().equals("start")) {
            GuildMessageChannel textChannelEvent = null;
            if (GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", event.getGuild().getId());

                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.GREEN);
                errors.setDescription(messageGiftNeedStopGiveaway);

                event.replyEmbeds(errors.build()).queue();
            } else {
                GuildChannelUnion textChannel = event.getOption("textchannel", OptionMapping::getAsChannel);

                //Если не указали конкретный канал
                if (textChannel == null) {
                    if (event.getChannel() instanceof TextChannel) {
                        textChannelEvent = event.getChannel().asTextChannel();
                        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                        if (!canSendGiveaway) return;
                    } else if (event.getChannel() instanceof NewsChannel) {
                        textChannelEvent = event.getChannel().asNewsChannel();
                        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                        if (!canSendGiveaway) return;
                    }
                    //Если указывали
                } else {
                    if (textChannel instanceof TextChannel) {
                        textChannelEvent = textChannel.asTextChannel();
                        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                        if (!canSendGiveaway) return;
                    } else if (textChannel instanceof NewsChannel) {
                        textChannelEvent = textChannel.asNewsChannel();
                        boolean canSendGiveaway = ChecksClass.canSendGiveaway(textChannelEvent, event);
                        if (!canSendGiveaway) return;
                    } else {
                        String startInNotTextChannels = jsonParsers.getLocale("start_in_not_text_channels", event.getGuild().getId());
                        event.reply(startInNotTextChannels).queue();
                        return;
                    }
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
                    embedBuilder.setColor(Color.BLACK);

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

                    if (role != null && role == guildIdLong && isOnlyForSpecificRole) {
                        String slashErrorRoleCanNotBeEveryone = jsonParsers.getLocale("slash_error_role_can_not_be_everyone", event.getGuild().getId());
                        embedBuilder.setDescription(slashErrorRoleCanNotBeEveryone);
                        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
                        return;
                    }

                    Gift gift = new Gift(guildIdLong,
                            textChannelEvent.getIdLong(),
                            event.getUser().getIdLong(),
                            activeGiveawayRepository,
                            participantsRepository,
                            listUsersRepository);


                    GiveawayRegistry.getInstance().putGift(guildIdLong, gift);

                    String sendSlashMessage = String.format(jsonParsers.getLocale("send_slash_message", event.getGuild().getId()), textChannelEvent.getId());

                    try {
                        event.reply(sendSlashMessage)
                                .delay(5, TimeUnit.SECONDS)
                                .flatMap(InteractionHook::deleteOriginal)
                                .queue();
                    } catch (Exception ignored) {
                    }

                    gift.startGift(event.getGuild(),
                            textChannelEvent,
                            title,
                            count,
                            time,
                            role,
                            isOnlyForSpecificRole,
                            urlImage,
                            event.getUser().getIdLong());

                    //Мы не будет очищать это, всё равно рано или поздно будет перезаписываться или даже не будет в случае Exception
                    GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(guildIdLong, event.getUser().getIdLong());

                    //Если время будет неверным. Сработает try catch
                } catch (Exception e) {
                    if (!e.getMessage().contains("Time in the past")) {
                        e.printStackTrace();
                    }
                    String slashErrors = jsonParsers.getLocale("slash_errors", event.getGuild().getId());
                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(Color.GREEN);
                    errors.setDescription(slashErrors);
                    event.getHook().editOriginalEmbeds(errors.build()).queue();
                    GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildIdLong);
                    activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
                }
            }
            return;
        }

        if (event.getName().equals("stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", event.getGuild().getId());

                EmbedBuilder notHas = new EmbedBuilder();
                notHas.setColor(Color.GREEN);
                notHas.setDescription(slashStopNoHas);

                event.replyEmbeds(notHas.build()).queue();
                return;
            }

            if (event.getMember() != null
                    && !event.getMember().hasPermission(event.getGuildChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                String messageGiftNotAdmin = jsonParsers.getLocale("message_gift_not_admin", event.getGuild().getId());

                EmbedBuilder gift = new EmbedBuilder();
                gift.setColor(Color.GREEN);
                gift.setDescription(messageGiftNotAdmin);

                event.replyEmbeds(gift.build()).setEphemeral(true).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                String slashStop = jsonParsers.getLocale("slash_stop", event.getGuild().getId());

                EmbedBuilder stop = new EmbedBuilder();
                stop.setColor(Color.GREEN);
                stop.setDescription(slashStop);

                event.replyEmbeds(stop.build()).queue();

                int count = GiveawayRegistry.getInstance().getCountWinners(guildIdLong);

                GiveawayRegistry.getInstance()
                        .getGift(guildIdLong)
                        .stopGift(guildIdLong, count);
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

            Gift gift = GiveawayRegistry.getInstance().getGift(guildIdLong);

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
                    .getGift(guildIdLong)
                    .stopGift(guildIdLong, Integer.parseInt(event.getOptions().get(0).getAsString()));
            return;
        }

        if (event.getName().equals("help")) {

            String guildIdLongString = event.getGuild().getId();

            EmbedBuilder info = new EmbedBuilder();
            info.setColor(Color.decode("#9900FF")); //Фиолетовый
            info.setTitle("Giveaway");
            info.addField("Slash Commands", "`/language`, `/start`, `/stop`, `/list`" +
                    "\n`/reroll`, `/participants`, `/patreon`", false);

            String messagesEventsLinks = jsonParsers.getLocale("messages_events_links", guildIdLongString);
            String messagesEventsSite = jsonParsers.getLocale("messages_events_site", guildIdLongString);
            String messagesEventsAddMeToOtherGuilds = jsonParsers.getLocale("messages_events_add_me_to_other_guilds", guildIdLongString);

            info.addField(messagesEventsLinks, messagesEventsSite + messagesEventsAddMeToOtherGuilds, false);

            List<Button> buttons = new ArrayList<>();

            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

            if (BotStartConfig.getMapLanguages().get(guildIdLongString) != null) {

                if (BotStartConfig.getMapLanguages().get(guildIdLongString).equals("eng")) {

                    buttons.add(Button.secondary(guildIdLongString + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                    "Сменить язык ")
                            .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
                } else {
                    buttons.add(Button.secondary(guildIdLongString + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                    "Change language ")
                            .withEmoji(Emoji.fromUnicode("U+1F1ECU+1F1E7")));
                }
            } else {
                buttons.add(Button.secondary(guildIdLongString + ":" + ReactionsButton.CHANGE_LANGUAGE,
                                "Сменить язык ")
                        .withEmoji(Emoji.fromUnicode("U+1F1F7U+1F1FA")));
            }

            event.replyEmbeds(info.build()).setEphemeral(true).addActionRow(buttons).queue();
            return;
        }

        //0 - bot
        if (event.getName().equals("language")) {

            if (event.getMember() != null && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {

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

            if (GiveawayRegistry.getInstance().hasGift(guildIdLong)) {

                StringBuilder stringBuilder = new StringBuilder();

                List<Participants> participantsList = participantsRepository.getParticipantsByGuildIdLong(guildIdLong);

                if (participantsList.isEmpty()) {
                    String slashListUsersEmpty = jsonParsers.getLocale("slash_list_users_empty", event.getGuild().getId());

                    EmbedBuilder list = new EmbedBuilder();
                    list.setColor(Color.GREEN);
                    list.setDescription(slashListUsersEmpty);
                    event.replyEmbeds(list.build()).setEphemeral(true).queue();
                    return;
                }

                for (Participants participants : participantsList) {
                    if (stringBuilder.length() < 4000) {
                        stringBuilder.append(stringBuilder.length() == 0 ? "<@" : ", <@").append(participants.getUserIdLong()).append(">");
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

        if (event.getName().equals("reroll")) {
            event.deferReply().queue();
            OptionMapping option = event.getOption("id");

            if (option != null) {

                if (!option.getAsString().matches("\\d+")) {
                    event.getHook().sendMessage("ID is not Number!").setEphemeral(true).queue();
                    return;
                }

                String id = event.getOption("id", OptionMapping::getAsString);

                if (GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                    String messageGiftNeedStopGiveaway = jsonParsers.getLocale("message_gift_need_stop_giveaway", event.getGuild().getId());

                    EmbedBuilder giveaway = new EmbedBuilder();
                    giveaway.setColor(Color.orange);
                    giveaway.setDescription(messageGiftNeedStopGiveaway);
                    event.getHook().sendMessageEmbeds(giveaway.build()).setEphemeral(true).queue();
                    return;
                }

                try {
                    List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayId(Long.valueOf(id));

                    Winners winners = new Winners(1, 0, listUsers.size() - 1);

                    String[] setWinners = api.setWinners(winners);

                    final Set<String> uniqueWinners = new LinkedHashSet<>();

                    for (String setWinner : setWinners) {
                        uniqueWinners.add("<@" + listUsers.get(Integer.parseInt(setWinner)).getUserIdLong() + ">");
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
                } catch (Exception ex) {
                    Exceptions.handle(ex, event.getHook());
                    return;
                }
            } else {
                event.getHook().sendMessage("Options is null").setEphemeral(true).queue();
            }
            return;
        }

        if (event.getName().equals("change")) {
            long guildId = event.getGuild().getIdLong();
            String guildIdString = event.getGuild().getId();

            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            if (!instance.hasGift(guildId)) {
                String slashStopNoHas = jsonParsers.getLocale("slash_stop_no_has", guildIdString);
                event.reply(slashStopNoHas).setEphemeral(true).queue();
                return;
            }

            String time = event.getOption("duration", OptionMapping::getAsString);
            EmbedBuilder start = new EmbedBuilder();

            Gift gift = instance.getGift(guildId);
            String title = instance.getTitle(guildId);
            Long role = instance.getRoleId(guildId);
            boolean isOnlyForSpecificRole = instance.getIsForSpecificRole(guildId);
            int countWinners = instance.getCountWinners(guildId);
            String urlImage = instance.getUrlImage(guildId);
            long userWhoCreateGiveaway = instance.getIdUserWhoCreateGiveaway(guildId);
            long channelId = gift.getTextChannelId();
            long messageId = instance.getMessageId(guildId);

            String giftReaction = jsonParsers.getLocale("gift_reaction", guildIdString);

            start.setColor(Color.GREEN);
            start.setTitle(title);
            start.appendDescription(giftReaction);

            if (role != null && role != 0L && isOnlyForSpecificRole) {
                String giftOnlyFor = String.format(jsonParsers.getLocale("gift_only_for", guildIdString), role);
                start.appendDescription(giftOnlyFor);
            }

            String footer;
            if (countWinners == 1) {
                footer = String.format("1 %s", GiftHelper.setEndingWord(1, guildId));
            } else {
                footer = String.format("%s %s", countWinners, GiftHelper.setEndingWord(countWinners, guildId));
            }

            start.setFooter(footer);

            if (time != null) {
                gift.setTime(start, time, footer);
            }

            String hosted = String.format("\nHosted by: <@%s>", userWhoCreateGiveaway);
            start.appendDescription(hosted);

            if (urlImage != null) {
                start.setImage(urlImage);
            }
            Timestamp endGiveawayDate = instance.getEndGiveawayDate(guildId);

            activeGiveawayRepository.updateGiveawayTime(guildIdLong, endGiveawayDate);
            EditMessage.edit(start.build(), guildId, channelId, messageId);

            String changeDuration = jsonParsers.getLocale("change_duration", guildIdString);
            event.reply(changeDuration).setEphemeral(true).queue();
            return;
        }

        if (event.getName().equals("participants")) {
            event.deferReply().setEphemeral(true).queue();
            String id = event.getOption("id", OptionMapping::getAsString);

            File file = new File("participants.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                List<ListUsers> listUsers = listUsersRepository.findAllByGiveawayId(Long.parseLong(id));

                String json = gson.toJson(listUsers);

                // Создание объекта FileWriter
                FileWriter writer = new FileWriter(file);

                // Запись содержимого в файл
                writer.write(json);
                writer.flush();
                writer.close();

                FileUpload fileUpload = FileUpload.fromData(file);

                event.getHook().sendFiles(fileUpload).setEphemeral(true).queue();
            } catch (Exception exception) {
                Exceptions.handle(exception, event.getHook());
                return;
            }
            return;
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