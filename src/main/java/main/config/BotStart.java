package main.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.core.events.ReactionEvent;
import main.giveaway.*;
import main.jsonparser.ParserClass;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.entity.Settings;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import main.model.repository.SettingsRepository;
import main.service.SavingParticipantsService;
import main.service.ScheduleStartService;
import main.service.StopGiveawayService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.boticordjava.api.entity.bot.stats.BotStats;
import org.boticordjava.api.impl.BotiCordAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class BotStart {
    private final static Logger LOGGER = LoggerFactory.getLogger(BotStart.class.getName());

    public static final String activity = "/help | ";
    //String - guildLongId
    private static final ConcurrentMap<Long, Settings> mapLanguages = new ConcurrentHashMap<>();

    @Getter
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //API
    private final BotiCordAPI api = new BotiCordAPI.Builder()
            .token(Config.getBoticord())
            .enableDevMode()
            .build();

    //REPOSITORY
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;
    private final UpdateController updateController;
    private final SettingsRepository settingsRepository;

    //Service
    private final ScheduleStartService scheduleStartService;
    private final StopGiveawayService stopGiveawayService;
    private final SavingParticipantsService savingParticipantsService;
    private final GiveawayEnd giveawayEnd;
    private final GiveawaySaving giveawaySaving;
    private final GiveawayMessageHandler giveawayMessageHandler;

    @PostConstruct
    public synchronized void startBot() {
        try {
            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();
            //Устанавливаем языки
            setLanguages();
            getLocalizationFromDB();
            //Получаем Giveaway и пользователей. Устанавливаем данные
            setGiveawayAndUsersInGift();

            List<GatewayIntent> intents = new ArrayList<>(
                    Arrays.asList(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGE_TYPING));

            List<CacheFlag> cacheFlags = new ArrayList<>(
                    Arrays.asList(
                            CacheFlag.ROLE_TAGS,
                            CacheFlag.ACTIVITY,
                            CacheFlag.MEMBER_OVERRIDES));

            jdaBuilder.disableCache(cacheFlags);
            jdaBuilder.enableIntents(intents);
            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new CoreBot(updateController));

            jda = jdaBuilder.build();
            jda.awaitReady();

            List<Command> complete = jda.retrieveCommands().complete();
            complete.forEach(command -> System.out.println(command.toString()));

            System.out.println("IsDevMode: " + Config.isIsDev());

            //Обновить команды
//            updateSlashCommands();
            System.out.println("20:22");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "giveaway_id", "Giveaway ID")
                    .setName("giveaway_id")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "count", "Examples: 1, 2... If not specified -> default value at startup")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 1, 2... Если не указано -> стандартное значение при запуске"));

            List<OptionData> optionsSettings = new ArrayList<>();
            optionsSettings.add(new OptionData(STRING, "language", "Setting the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            optionsSettings.add(new OptionData(STRING, "color", "Embed color: #00FF00")
                    .setName("color")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Embed цвет: #00FF00"));

            //Scheduling Giveaway
            List<OptionData> optionsScheduling = new ArrayList<>();

            optionsScheduling.add(new OptionData(STRING, "start_time", "Examples: 2023.04.29 16:00. Only in this style and UTC ±0")
                    .setName("start_time")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 2023.04.29 16:00. Только в этом стиле и UTC ±0"));

            optionsScheduling.add(new OptionData(CHANNEL, "channel", "Choose #TextChannel")
                    .setName("textchannel")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выбрать #TextChannel"));

            optionsScheduling.add(new OptionData(STRING, "end_time", "Examples: 2023.04.29 17:00. Only in this style and UTC ±0")
                    .setName("end_time")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 2023.04.29 17:00. Только в этом стиле и UTC ±0"));

            optionsScheduling.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "count", "Set count winners. Default 1")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsScheduling.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsScheduling.add(new OptionData(STRING, "role", "Giveaway is only for a specific role? Don't forget to specify the Role in the previous choice.")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен только для определенной роли? Не забудьте указать роль в предыдущем выборе."));

            optionsScheduling.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "min_participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min_participants")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "count", "Set count winners. Default 1")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsStart.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsStart.add(new OptionData(STRING, "role", "Giveaway is only for a specific role? Don't forget to specify the Role in the previous choice.")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен только для определенной роли? Не забудьте указать роль в предыдущем выборе."));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "min_participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min_participants")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            List<OptionData> predefined = new ArrayList<>();
            predefined.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway")
                    .setRequired(true));
            predefined.add(new OptionData(INTEGER, "count", "Set count winners")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));
            predefined.add(new OptionData(ROLE, "role", "Installing a @Role for collecting")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установка @Роли для сбора")
                    .setRequired(true));

            //change
            List<OptionData> change = new ArrayList<>();
            change.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "giveaway_id", "Giveaway ID")
                    .setName("giveaway_id")
                    .setRequired(true).setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "textchannel", "Checking the permissions of a specific channel")
                    .setName("textchannel")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            commands.addCommands(Commands.slash("check-bot-permission", "Checking the permission bot")
                    .addOptions(botPermissions)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота"));

            commands.addCommands(Commands.slash("settings", "Bot settings")
                    .addOptions(optionsSettings)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройки бота")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)));

            commands.addCommands(Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway"));

            commands.addCommands(Commands.slash("scheduling", "Create Scheduling Giveaway")
                    .addOptions(optionsScheduling)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создать Giveaway по расписанию"));

            commands.addCommands(Commands.slash("stop", "Stop the Giveaway")
                    .addOptions(optionsStop)
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить Giveaway"));

            commands.addCommands(Commands.slash("help", "Bot commands")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота"));

            commands.addCommands(Commands.slash("list", "List of participants")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Список участников"));

            commands.addCommands(Commands.slash("patreon", "Support us on Patreon")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Поддержите нас на Patreon"));

            commands.addCommands(Commands.slash("participants", "Get file with all participants")
                    .addOptions(participants)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить файл со всеми участниками"));

            commands.addCommands(Commands.slash("reroll", "Reroll one winner")
                    .addOptions(reroll)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Перевыбрать одного победителя"));

            commands.addCommands(Commands.slash("change", "Change the time")
                    .addOptions(change)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Изменить время")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

            commands.addCommands(Commands.slash("predefined", "Gather participants and immediately hold a drawing for a certain @Role")
                    .addOptions(predefined)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Собрать участников и сразу провести розыгрыш для определенной @Роли")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

            commands.addCommands(Commands.slash("cancel", "Cancel Giveaway")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Отменить Giveaway"));

            commands.queue();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 120, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void topGGAndStatcord() {
        if (!Config.isIsDev()) {
            try {
                int serverCount = BotStart.jda.getGuilds().size();
                BotStart.jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));

                //BOTICORD API
                AtomicInteger usersCount = new AtomicInteger();
                BotStart.jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

                BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
                api.setBotStats(Config.getBotId(), botStats);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void scheduleStartGiveaway() {
        scheduleStartService.start(jda);
    }

    @Scheduled(fixedDelay = 2, initialDelay = 2, timeUnit = TimeUnit.SECONDS)
    private void stopGiveaway() {
        stopGiveawayService.stop();
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void savingParticipants() {
        savingParticipantsService.save();
    }

    private void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);

                for (Object o : jsonObject.keySet()) {
                    String key = (String) o;

                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(key, String.valueOf(jsonObject.get(key)));
                    } else {
                        ParserClass.english.put(key, String.valueOf(jsonObject.get(key)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void setGiveawayAndUsersInGift() {
        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll();

        for (ActiveGiveaways activeGiveaways : activeGiveawaysList) {
            try {
                long guildId = activeGiveaways.getGuildLongId();
                long channelIdLong = activeGiveaways.getChannelIdLong();
                int countWinners = activeGiveaways.getCountWinners();
                long messageIdLong = activeGiveaways.getMessageIdLong();
                String title = activeGiveaways.getGiveawayTitle();
                Timestamp dateEndGiveaway = activeGiveaways.getDateEndGiveaway();
                Long role = activeGiveaways.getRoleIdLong(); // null -> 0
                boolean isForSpecificRole = activeGiveaways.getIsForSpecificRole();
                String urlImage = activeGiveaways.getUrlImage();
                long idUserWhoCreateGiveaway = activeGiveaways.getIdUserWhoCreateGiveaway();
                Integer minParticipants = activeGiveaways.getMinParticipants();

                Map<String, String> participantsMap = new HashMap<>();
                Set<Participants> participantsList = activeGiveaways.getParticipants();

                participantsList.forEach(participants -> {
                            String userIdAsString = participants.getUserIdAsString();
                            participantsMap.put(userIdAsString, userIdAsString);
                        }
                );

                GiveawayBuilder.Builder giveawayBuilder = new GiveawayBuilder.Builder();
                giveawayBuilder.setGiveawayEnd(giveawayEnd);
                giveawayBuilder.setActiveGiveawayRepository(activeGiveawayRepository);
                giveawayBuilder.setGiveawaySaving(giveawaySaving);
                giveawayBuilder.setParticipantsRepository(participantsRepository);
                giveawayBuilder.setListUsersRepository(listUsersRepository);
                giveawayBuilder.setGiveawayMessageHandler(giveawayMessageHandler);

                giveawayBuilder.setTextChannelId(channelIdLong);
                giveawayBuilder.setUserIdLong(idUserWhoCreateGiveaway);
                giveawayBuilder.setMessageId(messageIdLong);
                giveawayBuilder.setGuildId(guildId);
                giveawayBuilder.setTitle(title);
                giveawayBuilder.setCountWinners(countWinners);
                giveawayBuilder.setRoleId(role);
                giveawayBuilder.setEndGiveawayDate(dateEndGiveaway);
                giveawayBuilder.setForSpecificRole(isForSpecificRole);
                giveawayBuilder.setUrlImage(urlImage);
                giveawayBuilder.setMinParticipants(minParticipants);
                giveawayBuilder.setListUsersHash(participantsMap);

                Giveaway giveaway = giveawayBuilder.build();
                giveaway.setLockEnd(true);
                GiveawayRegistry.getInstance().putGift(guildId, giveaway);

                if (dateEndGiveaway != null) {
                    updateGiveawayByGuild(giveaway);
                    giveaway.setLockEnd(false);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            System.out.println("getMessageIdFromDB()");
        }
    }

    @Scheduled(fixedDelay = 240000, initialDelay = 25000)
    public synchronized void updateUserList() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayDataList = new LinkedList<>(instance.getGiveaways());
        for (Giveaway giveaway : giveawayDataList) {
            try {
                updateGiveawayByGuild(giveaway);
                Thread.sleep(1000L);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Async
    public synchronized void updateGiveawayByGuild(Giveaway giveawayData) {
        long guildIdLong = giveawayData.getGuildId();
        boolean isForSpecificRole = giveawayData.isForSpecificRole();
        long messageId = giveawayData.getMessageId();

        if (jda != null) {
            if (hasGift(guildIdLong)) {
                long channelId = giveawayData.getTextChannelId();
                //System.out.println("Guild ID: " + guildIdLong);

                List<MessageReaction> reactions = null;
                TextChannel textChannelById;
                try {
                    Guild guildById = jda.getGuildById(guildIdLong);
                    if (guildById != null) {
                        textChannelById = guildById.getTextChannelById(channelId);
                        if (textChannelById != null) {
                            reactions = textChannelById
                                    .retrieveMessageById(messageId)
                                    .complete()
                                    .getReactions()
                                    .stream()
                                    .filter(messageReaction -> messageReaction.getEmoji().getName().equals(ReactionEvent.TADA))
                                    .toList();
                        }

                        //-1 because one Bot
                        if (hasGift(guildIdLong) &&
                                reactions != null &&
                                !reactions.isEmpty() &&
                                reactions.get(0).getCount() - 1 != giveawayData.getListUsersSize()) {
                            for (MessageReaction reaction : reactions) {
                                Map<String, User> userList = reaction
                                        .retrieveUsers()
                                        .complete()
                                        .stream()
                                        .filter(user -> !user.isBot())
                                        .filter(user -> !giveawayData.isUserContains(user.getId()))
                                        .collect(Collectors.toMap(User::getId, user -> user));

                                if (isForSpecificRole) {
                                    try {
                                        Map<String, User> localUserMap = new HashMap<>(userList); //bad practice but it`s work
                                        Role roleGiveaway = jda.getRoleById(giveawayData.getRoleId());
                                        for (Map.Entry<String, User> entry : localUserMap.entrySet()) {
                                            Guild guild = jda.getGuildById(guildIdLong);
                                            if (guild != null) {
                                                try {
                                                    Member member = guild.retrieveMemberById(entry.getKey()).complete();
                                                    if (member != null) {
                                                        boolean contains = member.getRoles().contains(roleGiveaway);
                                                        if (!contains) {
                                                            userList.remove(entry.getKey());
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    //Если пользователя нет в Гильдии удаляем из списка
                                                    if (e.getMessage().contains("10007: Unknown Member")) {
                                                        userList.remove(entry.getKey());
                                                    } else {
                                                        LOGGER.error(e.getMessage(), e);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error(e.getMessage(), e);
                                    }
                                }

                                //System.out.println("UserList count: " + userList);
                                //Перебираем Users в реакциях
                                for (Map.Entry<String, User> entry : userList.entrySet()) {
                                    if (!hasGift(guildIdLong)) return;
                                    giveawayData.addUser(entry.getValue());
                                    //System.out.println("User id: " + user.getIdLong());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("10008: Unknown Message")
                            || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                        System.out.println("updateUserList() " + e.getMessage() + " удаляем!");
                        activeGiveawayRepository.deleteById(guildIdLong);
                        GiveawayRegistry.getInstance().removeGiveaway(guildIdLong);
                    } else {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void getLocalizationFromDB() {
        try {
            List<Settings> languageList = settingsRepository.findAll();
            languageList.forEach(language -> mapLanguages.put(language.getServerId(), language));
            System.out.println("getLocalizationFromDB()");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean hasGift(long guildIdLong) {
        return GiveawayRegistry.getInstance().hasGiveaway(guildIdLong);
    }

    public static Map<Long, Settings> getMapLanguages() {
        return mapLanguages;
    }
}