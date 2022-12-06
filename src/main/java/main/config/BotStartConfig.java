package main.config;

import main.events.MessageWhenBotJoinToGuild;
import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.giveaway.buttons.ReactionsButton;
import main.giveaway.reactions.Reactions;
import main.giveaway.slash.SlashCommand;
import main.jsonparser.ParserClass;
import main.model.entity.Notification;
import main.model.entity.Participants;
import main.model.repository.*;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.boticordjava.api.entity.Enums.TokenEnum;
import org.boticordjava.api.impl.BotiCordAPI;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Configuration
@EnableScheduling
public class BotStartConfig {

    public static final String activity = "/help | ";
    //String - guildLongId
    private static final ConcurrentMap<String, String> mapLanguages = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Notification.NotificationStatus> mapNotifications = new ConcurrentHashMap<>();

    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //API
    private final BotiCordAPI api = new BotiCordAPI.Builder()
            .tokenEnum(TokenEnum.BOT)
            .token(System.getenv("BOTICORD"))
            .enableDevMode()
            .build();

    private final DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
            .token(Config.getTopGgApiToken())
            .botId(Config.getBotId()).build();

    //REPOSITORY
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;
    private final ParticipantsRepository participantsRepository;
    private final NotificationRepository notificationRepository;
    private final ListUsersRepository listUsersRepository;

    //DataBase
    @Value("${spring.datasource.url}")
    private String URL_CONNECTION;
    @Value("${spring.datasource.username}")
    private String USER_CONNECTION;
    @Value("${spring.datasource.password}")
    private String PASSWORD_CONNECTION;

    @Autowired
    public BotStartConfig(ActiveGiveawayRepository activeGiveawayRepository, LanguageRepository
            languageRepository, ParticipantsRepository participantsRepository, NotificationRepository notificationRepository, ListUsersRepository listUsersRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.languageRepository = languageRepository;
        this.participantsRepository = participantsRepository;
        this.notificationRepository = notificationRepository;
        this.listUsersRepository = listUsersRepository;
    }

    @Bean
    public synchronized void startBot() {
        try {
            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();

            //Устанавливаем языки
            setLanguages();

            getLocalizationFromDB();

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
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild(activeGiveawayRepository, languageRepository));
            jdaBuilder.addEventListeners(new ReactionsButton(languageRepository, notificationRepository));
            jdaBuilder.addEventListeners(new Reactions());
            jdaBuilder.addEventListeners(new SlashCommand(languageRepository, activeGiveawayRepository, participantsRepository, listUsersRepository));

            jda = jdaBuilder.build();
            jda.awaitReady();

            //Получаем Giveaway и пользователей. Устанавливаем данные
            setGiveawayAndUsersInGift();

            System.out.println(jda.retrieveCommands().complete());
            //Обновляем список участников при запуске бота

            System.out.println("updateUserList()");
            System.out.println("IsDevMode: " + Config.isIsDev());

            //Обновить команды
//            updateSlashCommands();
            System.out.println("20:14");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "id", "Giveaway ID")
                    .setName("id")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "count", "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1"));

            //Set language
            List<OptionData> optionsLanguage = new ArrayList<>();
            optionsLanguage.add(new OptionData(STRING, "bot", "Setting the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway. Maximum 255 characters")
                    .setName("title")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway. Максимум 255 символов"));

            optionsStart.add(new OptionData(INTEGER, "count", "Set count winners")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей"));

            optionsStart.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            optionsStart.add(new OptionData(CHANNEL, "channel", "#TextChannel name")
                    .setName("textchannel")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "#TextChannel название"));

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

            List<OptionData> predefined = new ArrayList<>();
            predefined.add(new OptionData(STRING, "title", "Title for Giveaway. Maximum 255 characters")
                    .setName("title")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway. Максимум 255 символов")
                    .setRequired(true));
            predefined.add(new OptionData(INTEGER, "count", "Set count winners")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));
            predefined.add(new OptionData(ROLE, "role", "Installing a @Role for collecting")
                    .setName("set")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установка @Роли для сбора")
                    .setRequired(true));

            //change
            List<OptionData> change = new ArrayList<>();
            change.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "id", "Giveaway ID")
                    .setName("id")
                    .setRequired(true).setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "textchannel", "Checking the permissions of a specific channel")
                    .setName("textchannel")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            commands.addCommands(Commands.slash("check-bot-permission", "Checking the permission bot")
                    .addOptions(botPermissions)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота"));

            commands.addCommands(Commands.slash("language", "Setting language")
                    .addOptions(optionsLanguage)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)));

            commands.addCommands(Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway"));

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

            commands.queue();

            System.out.println("Готово");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 900000L, initialDelay = 8000L)
    private void topGGAndStatcord() {
        if (!Config.isIsDev()) {
            try {
                int serverCount = BotStartConfig.jda.getGuilds().size();

                TOP_GG_API.setStats(serverCount);
                BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));

                //BOTICORD API
                AtomicInteger usersCount = new AtomicInteger();
                BotStartConfig.jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

                api.setStats(serverCount, 1, usersCount.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            e.printStackTrace();
        }
    }

    private void setGiveawayAndUsersInGift() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);

            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM active_giveaways";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {

                long guild_long_id = rs.getLong("guild_long_id");
                long channel_long_id = rs.getLong("channel_id_long");
                int count_winners = rs.getInt("count_winners");
                long message_id_long = rs.getLong("message_id_long");
                String giveaway_title = rs.getString("giveaway_title");
                Timestamp date_end_giveaway = rs.getTimestamp("date_end_giveaway");
                Long role_id_long = rs.getLong("role_id_long"); // null -> 0
                boolean is_for_specific_role = rs.getBoolean("is_for_specific_role");
                String url_image = rs.getString("url_image");
                long id_user_who_create_giveaway = rs.getLong("id_user_who_create_giveaway");

                Map<String, String> participantsList = participantsRepository
                        .getParticipantsByGuildIdLong(guild_long_id)
                        .stream()
                        .collect(Collectors.toMap(Participants::getUserIdAsString, Participants::getUserIdAsString));

                Gift gift = new Gift(guild_long_id,
                        channel_long_id,
                        id_user_who_create_giveaway,
                        //Добавляем пользователей в hashmap
                        participantsList,
                        activeGiveawayRepository,
                        participantsRepository,
                        listUsersRepository);

                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.putGift(guild_long_id, gift);

                //Устанавливаем счетчик на верное число
                instance.getGift(guild_long_id).setCount(participantsList.size());

                instance.putMessageId(guild_long_id, message_id_long);
                instance.putTitle(guild_long_id, giveaway_title == null ? "Giveaway" : giveaway_title);
                instance.putEndGiveawayDate(guild_long_id, date_end_giveaway);
                instance.putChannelId(guild_long_id, channel_long_id);
                instance.putCountWinners(guild_long_id, count_winners);
                instance.putRoleId(guild_long_id, role_id_long);
                instance.putIsForSpecificRole(guild_long_id, is_for_specific_role);
                instance.putUrlImage(guild_long_id, url_image);
                instance.putIdUserWhoCreateGiveaway(guild_long_id, id_user_who_create_giveaway);

                if (date_end_giveaway != null) {
                    Timer timer = new Timer();
                    StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(guild_long_id);
                    Date date = new Date(date_end_giveaway.getTime());
                    timer.schedule(stopGiveawayByTimer, date);

                    instance.putGiveawayTimer(guild_long_id, stopGiveawayByTimer, timer);
                }
            }
            rs.close();
            statement.close();
            connection.close();
            System.out.println("getMessageIdFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 240000, initialDelay = 17000)
    public void updateUserList() throws InterruptedException {
        List<Gift> giveawayDataList = new LinkedList<>(GiveawayRegistry.getAllGift());

        for (Gift giveawayData : giveawayDataList) {

            long guildIdLong = giveawayData.getGuildId();
            boolean isForSpecificRole = giveawayData.getGiveawayData().getIsForSpecificRole();
            long messageId = GiveawayRegistry.getInstance().getMessageId(guildIdLong);

            if (hasGift(guildIdLong)) {

                long channelId = GiveawayRegistry.getInstance().getGift(guildIdLong).getTextChannelId();
                Gift gift = GiveawayRegistry.getInstance().getGift(guildIdLong);

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
                                    .filter(messageReaction -> messageReaction.getEmoji().getName().equals(Reactions.TADA))
                                    .toList();
                        }

                        //-1 because one Bot
                        if (hasGift(guildIdLong) &&
                                reactions != null &&
                                reactions.size() > 0 &&
                                reactions.get(0).getCount() - 1 != gift.getListUsersSize()) {
                            for (MessageReaction reaction : reactions) {
                                Map<String, User> userList = reaction
                                        .retrieveUsers()
                                        .complete()
                                        .stream()
                                        .filter(user -> !user.isBot())
                                        .filter(user -> !gift.hasUserInGiveaway(user.getId()))
                                        .collect(Collectors.toMap(User::getId, user -> user));

                                if (isForSpecificRole) {
                                    try {
                                        Map<String, User> userMapTemp = new HashMap<>(userList); //bad practice but it`s work
                                        Role roleGiveaway = jda.getRoleById(giveawayData.getGiveawayData().getRoleId());
                                        for (Map.Entry<String, User> entry : userMapTemp.entrySet()) {
                                            Guild guild = jda.getGuildById(guildIdLong);
                                            if (guild != null) {
                                                try {
                                                    Member member = guild.retrieveMember(entry.getValue()).complete();
                                                    if (member != null) {
                                                        boolean contains = member.getRoles().contains(roleGiveaway);
                                                        if (!contains) {
                                                            userList.remove(entry.getKey());
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    if (e.getMessage().contains("10007: Unknown Member")) {
                                                        System.out.println("10007: Unknown Member");
                                                        userList.remove(entry.getKey());
                                                    } else {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                //System.out.println("UserList count: " + userList);
                                //Перебираем Users в реакциях
                                for (Map.Entry<String, User> entry : userList.entrySet()) {
                                    if (!hasGift(guildIdLong)) return;
                                    gift.addUserToPoll(entry.getValue());
                                    //System.out.println("User id: " + user.getIdLong());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("10008: Unknown Message") || e.getMessage().contains("Missing permission: VIEW_CHANNEL")) {
                        System.out.println("updateUserList() " + e.getMessage() + " удаляем!");
                        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
                        GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildIdLong);
                    } else {
                        e.printStackTrace();
                    }
                }
            }
            try {
                //Что это такое и для чего?
                Gift.GiveawayTimerStorage giveawayTimer = GiveawayRegistry.getInstance().getGiveawayTimer(guildIdLong);
                if (giveawayTimer != null) {
                    StopGiveawayByTimer stopGiveawayByTimer = giveawayTimer.getStopGiveawayByTimer();
                    if (stopGiveawayByTimer.getCountDown() == 1) {
                        stopGiveawayByTimer.countDown();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Thread.sleep(2000L);
    }

    private void getLocalizationFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getString("server_id"), rs.getString("language"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getLocalizationFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean hasGift(long guildIdLong) {
        return GiveawayRegistry.getInstance().hasGift(guildIdLong);
    }

    public static Map<String, String> getMapLanguages() {
        return mapLanguages;
    }

    public static Map<String, Notification.NotificationStatus> getMapNotifications() {
        return mapNotifications;
    }

    public static JDA getJda() {
        return jda;
    }

}