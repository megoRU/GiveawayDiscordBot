package main.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayData;
import main.giveaway.GiveawayRegistry;
import main.giveaway.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.jsonparser.ParserClass;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.entity.Scheduling;
import main.model.entity.Settings;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.model.repository.SettingsRepository;
import main.service.GiveawayRepositoryService;
import main.service.GiveawayUpdateListUser;
import main.threads.StopGiveawayHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Configuration
@EnableScheduling
public class BotStart {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotStart.class.getName());

    private static final JSONParsers jsonParsers = new JSONParsers();
    public static final String activity = "/start | ";
    //String - guildLongId
    private static final ConcurrentMap<Long, Settings> mapLanguages = new ConcurrentHashMap<>();

    @Getter
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //REPOSITORY
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final UpdateController updateController;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;

    private final GiveawayRepositoryService giveawayRepositoryService;
    private final GiveawayUpdateListUser updateGiveawayByGuild;

    @Autowired
    public BotStart(ActiveGiveawayRepository activeGiveawayRepository,
                    UpdateController updateController,
                    SchedulingRepository schedulingRepository,
                    SettingsRepository settingsRepository,
                    GiveawayRepositoryService giveawayRepositoryService,
                    GiveawayUpdateListUser updateGiveawayByGuild) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.updateController = updateController;
        this.schedulingRepository = schedulingRepository;
        this.settingsRepository = settingsRepository;
        this.giveawayRepositoryService = giveawayRepositoryService;
        this.updateGiveawayByGuild = updateGiveawayByGuild;
    }

    @PostConstruct
    public void startBot() {
        try {
            CoreBot coreBot = new CoreBot(updateController);
            coreBot.init();

            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();
            //Устанавливаем языки
            setLanguages();
            getLocalizationFromDB();
            getSchedulingFromDB();

            List<GatewayIntent> intents = Arrays.asList(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.DIRECT_MESSAGE_TYPING);

            jdaBuilder.disableCache(
                    CacheFlag.ACTIVITY,
                    CacheFlag.VOICE_STATE,
                    CacheFlag.EMOJI,
                    CacheFlag.STICKER,
                    CacheFlag.CLIENT_STATUS,
                    CacheFlag.MEMBER_OVERRIDES,
                    CacheFlag.ROLE_TAGS,
                    CacheFlag.FORUM_TAGS,
                    CacheFlag.ONLINE_STATUS,
                    CacheFlag.SCHEDULED_EVENTS
            );

            jdaBuilder.enableIntents(intents);
            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new CoreBot(updateController));

            jda = jdaBuilder.build();
            jda.awaitReady();

            //Получаем Giveaway и пользователей. Устанавливаем данные
            setGiveawayAndUsersInGift();

            List<Command> complete = jda.retrieveCommands().complete();
            complete.forEach(command -> System.out.println(command.toString()));

            System.out.println("IsDevMode: " + Config.isIsDev());

            //Обновить команды
            updateSlashCommands();
            System.out.println("20:22");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void getSchedulingFromDB() {
        try {
            List<Scheduling> schedulingList = schedulingRepository.findAll();
            GiveawayRegistry instance = GiveawayRegistry.getInstance();

            for (Scheduling scheduling : schedulingList) {
                String idSalt = scheduling.getIdSalt();
                instance.putScheduling(idSalt, scheduling);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to value set at start of Giveaway")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию установленное в Giveaway"));

            optionsStop.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Set language
            List<OptionData> optionsSettings = new ArrayList<>();
            optionsSettings.add(new OptionData(STRING, "language", "Set the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "язык")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            optionsSettings.add(new OptionData(STRING, "color", "Set the embed color. Example usage: `#ff00ff`")
                    .setName("color")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "цвет")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите цвет embed. Пример использования: `#ff00ff`"));

            //Scheduling Giveaway
            List<OptionData> optionsScheduling = new ArrayList<>();

            optionsScheduling.add(new OptionData(STRING, "start-time", "Set start time in UTC ±0 form. Example usage: 2023.04.29 17:00")
                    .setName("start-time")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-начала")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите время начала в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(CHANNEL, "channel", "Установите канал для Giveaway. По умолчанию канал в котором была запущена команда")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовый-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "По умолчанию текстовый канал, в котором была выполнена команда."));

            optionsScheduling.add(new OptionData(STRING, "end-time", "Set end time in UTC ±0 form. Example usage: 2023.04.29 17:00")
                    .setName("end-time")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-окончания")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Укажите время окончания в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsScheduling.add(new OptionData(ROLE, "mention", "Mention a specific @Role")
                    .setName("select")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "выбрать")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsScheduling.add(new OptionData(STRING, "role", "Set whether Giveaway is for a specific role. Role is set in previous selection")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен для определенной роли? Укажите роль в предыдущем выборе"));

            optionsScheduling.add(new OptionData(ATTACHMENT, "image", "Set image used in Giveaway embed")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is lesser than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsStart.add(new OptionData(STRING, "duration", "Set the duration. Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. UTC ±0")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить продолжительность. Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. UTC ±0"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "упомянуть")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsStart.add(new OptionData(STRING, "role", "Is Giveaway only for a specific role? Specify the role in the previous selection")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway только для определенной роли? Укажите роль в предыдущем выборе"));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            List<OptionData> predefined = new ArrayList<>();

            predefined.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway")
                    .setRequired(true));

            predefined.add(new OptionData(INTEGER, "winners", "Set number of winners")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));

            predefined.add(new OptionData(ROLE, "role", "Set @Role from which all participants are selected from")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите @Роль, из которой будут выбраны все участники")
                    .setRequired(true));

            //endmessage
            List<OptionData> endMessageDate = new ArrayList<>();

            endMessageDate.add(new OptionData(STRING, "text", "Set text. Must contain @winner to properly parse")
                    .setName("text")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текст")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Задать текст. Должен содержать @winner для правильного разбора"));

            //giveaway-edit
            List<OptionData> giveawayEditData = new ArrayList<>();

            giveawayEditData.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            giveawayEditData.add(new OptionData(INTEGER, "winners", "Set number of winners")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            giveawayEditData.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            giveawayEditData.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            giveawayEditData.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(1)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            giveawayEditData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            reroll.add(new OptionData(INTEGER, "winners", "Set count winners.")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "text-channel", "Check permissions of a specific channel")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовой-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            List<OptionData> cancelData = new ArrayList<>();
            cancelData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            /*
             * Команды
             */

            CommandData checkCommand = Commands.slash("check-bot-permission", "Check permissions of bot")
                    .addOptions(botPermissions)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота");

            CommandData settingsCommand = Commands.slash("settings", "Change bot settings")
                    .addOptions(optionsSettings)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройки бота")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData startCommand = Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway");

            CommandData schedulingCommand = Commands.slash("scheduling", "Create scheduled Giveaway")
                    .addOptions(optionsScheduling)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создать Giveaway по расписанию");

            CommandData stopCommand = Commands.slash("stop", "Stop the Giveaway and announce winners")
                    .addOptions(optionsStop)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить Giveaway");

            CommandData helpCommand = Commands.slash("help", "Bot commands")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота");

            CommandData patreonCommand = Commands.slash("patreon", "Support us on Patreon")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Поддержите нас на Patreon");

            CommandData participantsCommand = Commands.slash("participants", "Get file with all participants")
                    .addOptions(participants)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить файл со всеми участниками");

            CommandData endMessage = Commands.slash("endmessage", "Set message announcing the winners")
                    .addOptions(endMessageDate)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройте сообщение с объявлением победителей, заменяя его на указанный текст.");

            CommandData rerollCommand = Commands.slash("reroll", "Reroll winners")
                    .addOptions(reroll)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Перевыбрать одного победителя");

            CommandData giveawayEdit = Commands.slash("edit", "Change Giveaway settings")
                    .addOptions(giveawayEditData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Изменить настройки Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData predefinedCommand = Commands.slash("predefined", "Gather participants and immediately hold a drawing for a specific @Role")
                    .addOptions(predefined)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Собрать участников и сразу провести розыгрыш для определенной @Роли")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData listCommand = Commands.slash("list", "List of all active and scheduled Giveaways")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Список всех активных и запланированных Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData cancelCommand = Commands.slash("cancel", "Cancel Giveaway")
                    .addOptions(cancelData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Отменить Giveaway");

            commands.addCommands(
                            listCommand,
                            checkCommand,
                            endMessage,
                            settingsCommand,
                            startCommand,
                            schedulingCommand,
                            stopCommand,
                            helpCommand,
                            patreonCommand,
                            participantsCommand,
                            rerollCommand,
                            giveawayEdit,
                            predefinedCommand,
                            cancelCommand)
                    .queue();

            System.out.println("Готово");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 120, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void topGGAndStatcord() {
        if (!Config.isIsDev()) {
            int serverCount = BotStart.jda.getGuilds().size();
            BotStart.jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void scheduleStartGiveaway() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Collection<Scheduling> scheduledGiveaways = instance.getScheduledGiveaways();

        for (Scheduling scheduling : scheduledGiveaways) {
            Timestamp localTime = new Timestamp(System.currentTimeMillis());

            if (localTime.after(scheduling.getDateCreateGiveaway())) {
                try {
                    Long channelIdLong = scheduling.getChannelId();
                    Guild guildById = jda.getGuildById(scheduling.getGuildId());

                    if (guildById != null) {
                        TextChannel textChannelById = guildById.getTextChannelById(channelIdLong);
                        if (textChannelById != null) {
                            Long role = scheduling.getRoleId();
                            Boolean isOnlyForSpecificRole = scheduling.getIsForSpecificRole();
                            Long guildIdLong = scheduling.getGuildId();
                            Long guildId = scheduling.getGuildId();
                            String idSalt = scheduling.getIdSalt();

                            Giveaway giveaway = new Giveaway(
                                    scheduling.getGuildId(),
                                    textChannelById.getIdLong(),
                                    scheduling.getCreatedUserId(),
                                    giveawayRepositoryService,
                                    updateController);

                            String formattedDate = null;
                            if (scheduling.getDateEnd() != null) {
                                LocalDateTime dateEndGiveaway = LocalDateTime.ofInstant(scheduling.getDateEnd().toInstant(), ZoneOffset.UTC);
                                formattedDate = dateEndGiveaway.format(GiveawayUtils.FORMATTER);
                            }

                            if (role != null && isOnlyForSpecificRole) {
                                String giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_this_role", guildId), role);
                                if (Objects.equals(role, guildIdLong)) {
                                    giftNotificationForThisRole = String.format(jsonParsers.getLocale("gift_notification_for_everyone", guildId), "@everyone");
                                    textChannelById.sendMessage(giftNotificationForThisRole).submit().get();
                                } else {
                                    textChannelById.sendMessage(giftNotificationForThisRole).submit().get();
                                }
                            }

                            giveaway.startGiveaway(
                                    textChannelById,
                                    scheduling.getTitle(),
                                    scheduling.getCountWinners(),
                                    formattedDate,
                                    scheduling.getRoleId(),
                                    scheduling.getIsForSpecificRole(),
                                    scheduling.getUrlImage(),
                                    false,
                                    scheduling.getMinParticipants());

                            long messageId = giveaway.getGiveawayData().getMessageId();

                            instance.removeScheduling(idSalt); //Чтобы не моросил
                            instance.putGift(messageId, giveaway);

                            schedulingRepository.deleteByIdSalt(idSalt);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = new JSONObject(new JSONTokener(reader));

                for (String o : jsonObject.keySet()) {
                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(o, String.valueOf(jsonObject.get(o)));
                    } else {
                        ParserClass.english.put(o, String.valueOf(jsonObject.get(o)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
    }

    public void setGiveawayAndUsersInGift() {
        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll();

        for (ActiveGiveaways activeGiveaways : activeGiveawaysList) {
            try {
                long guild_long_id = activeGiveaways.getGuildId();
                long channel_long_id = activeGiveaways.getChannelId();
                int count_winners = activeGiveaways.getCountWinners();
                long message_id_long = activeGiveaways.getMessageId();
                String giveaway_title = activeGiveaways.getTitle();
                Timestamp date_end_giveaway = activeGiveaways.getDateEnd();
                Long role_id_long = activeGiveaways.getRoleId();
                boolean is_for_specific_role = Optional.ofNullable(activeGiveaways.getIsForSpecificRole()).orElse(false);
                String url_image = activeGiveaways.getUrlImage();
                long id_user_who_create_giveaway = activeGiveaways.getCreatedUserId();
                Integer min_participants = activeGiveaways.getMinParticipants();
                boolean finishGiveaway = activeGiveaways.isFinish();

                Map<String, String> participantsMap = new HashMap<>();
                Set<Participants> participantsList = activeGiveaways.getParticipants();

                participantsList.forEach(participants -> {
                            String userIdAsString = participants.getUserIdAsString();
                            participantsMap.put(userIdAsString, userIdAsString);
                        }
                );

                GiveawayData giveawayData = new GiveawayData(
                        message_id_long,
                        count_winners,
                        role_id_long,
                        is_for_specific_role,
                        url_image,
                        giveaway_title == null ? "Giveaway" : giveaway_title,
                        date_end_giveaway,
                        min_participants == null ? 1 : min_participants);

                giveawayData.setParticipantsList(participantsMap);

                Giveaway giveaway = new Giveaway(guild_long_id,
                        channel_long_id,
                        id_user_who_create_giveaway,
                        finishGiveaway,
                        true,
                        giveawayData,
                        giveawayRepositoryService,
                        updateController);

                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.putGift(message_id_long, giveaway);

                if (date_end_giveaway != null) {
                    updateGiveawayByGuild.updateGiveawayByGuild(giveaway);
                    giveaway.setLocked(false);
                }

                if (finishGiveaway) {
                    giveaway.stopGiveaway(count_winners);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            System.out.println("getMessageIdFromDB()");
        }
    }

    @Scheduled(fixedDelay = 2, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void stopGiveawayTimer() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayDataList = new LinkedList<>(instance.getAllGiveaway());
        StopGiveawayHandler stopGiveawayHandler = new StopGiveawayHandler();
        for (Giveaway giveaway : giveawayDataList) {
            try {
                stopGiveawayHandler.handleGiveaway(giveaway);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 150, initialDelay = 25, timeUnit = TimeUnit.SECONDS)
    public void updateUserList() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        List<Giveaway> giveawayDataList = new LinkedList<>(instance.getAllGiveaway());
        for (Giveaway giveaway : giveawayDataList) {
            try {
                updateGiveawayByGuild.updateGiveawayByGuild(giveaway);
                Thread.sleep(2000L);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void getLocalizationFromDB() {
        try {
            List<Settings> settingsList = settingsRepository.findAll();
            for (Settings settings : settingsList) {
                mapLanguages.put(settings.getServerId(), settings);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Map<Long, Settings> getMapLanguages() {
        return mapLanguages;
    }
}
