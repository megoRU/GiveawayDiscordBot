package main.config;

import main.events.MessageWhenBotJoinToGuild;
import main.giveaway.Gift;
import main.giveaway.GiveawayRegistry;
import main.giveaway.MessageGift;
import main.giveaway.api.response.ParticipantsPOJO;
import main.giveaway.api.response.StatsPOJO;
import main.giveaway.buttons.ReactionsButton;
import main.giveaway.slash.SlashCommand;
import main.jsonparser.ParserClass;
import main.messagesevents.LanguageChange;
import main.messagesevents.MessageInfoHelp;
import main.messagesevents.PrefixChange;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import main.model.repository.PrefixRepository;
import main.startbot.Statcord;
import main.threads.Giveaway;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.apache.commons.io.IOUtils;
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
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Configuration
@EnableScheduling
public class BotStartConfig {

    public static final String activity = "/help | ";
    private static final Deque<Giveaway> queue = new ArrayDeque<>();
    //String - guildLongId
    private static final ConcurrentMap<String, String> mapLanguages = new ConcurrentHashMap<>();
    private static final Map<String, String> mapPrefix = new HashMap<>();
    private static final Map<Integer, String> guildIdHashList = new HashMap<>();
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
    public static ExecutorService executorService;
    public static int serverCount;
    private volatile boolean isLaunched;

    //REPOSITORY
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;
    private final PrefixRepository prefixRepository;
    private final ParticipantsRepository participantsRepository;

    //DataBase
    @Value("${spring.datasource.url}")
    private String URL_CONNECTION;
    @Value("${spring.datasource.username}")
    private String USER_CONNECTION;
    @Value("${spring.datasource.password}")
    private String PASSWORD_CONNECTION;

    @Autowired
    public BotStartConfig(ActiveGiveawayRepository activeGiveawayRepository, LanguageRepository
            languageRepository, ParticipantsRepository participantsRepository, PrefixRepository prefixRepository) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.languageRepository = languageRepository;
        this.prefixRepository = prefixRepository;
        this.participantsRepository = participantsRepository;
    }

    @Bean
    public void startBot() {
        try {
            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();

            getLocalizationFromDB();
            //Устанавливаем языки
            setLanguages();
            //Получаем id guild и id message
            getMessageIdFromDB();
            //Получаем всех участников по гильдиям
            getUsersWhoTakePartFromDB();
            //Получаем все префиксы из базы данных
            getPrefixFromDB();

            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild(prefixRepository, activeGiveawayRepository, languageRepository));
            jdaBuilder.addEventListeners(new MessageGift(activeGiveawayRepository, participantsRepository));
            jdaBuilder.addEventListeners(new PrefixChange(prefixRepository));
            jdaBuilder.addEventListeners(new MessageInfoHelp());
            jdaBuilder.addEventListeners(new LanguageChange(languageRepository));
            jdaBuilder.addEventListeners(new ReactionsButton(languageRepository, participantsRepository, activeGiveawayRepository));
            jdaBuilder.addEventListeners(new SlashCommand(languageRepository, activeGiveawayRepository, participantsRepository));

            jda = jdaBuilder.build();
            jda.awaitReady();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(jda.retrieveCommands().complete());

        //Обновить команды
//        updateSlashCommands();
        System.out.println("15:25");
    }

    private void updateSlashCommands() {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            List<OptionData> optionsLanguage = new ArrayList<>();
            List<OptionData> optionsStart = new ArrayList<>();
            List<OptionData> optionsStop = new ArrayList<>();

            List<OptionData> option = new ArrayList<>();

            option.add(new OptionData(STRING, "id", "Giveaway ID").setName("id").setRequired(true));

            optionsLanguage.add(new OptionData(STRING, "bot", "Setting the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true));

            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway").setName("title"));

            optionsStart.add(new OptionData(INTEGER, "count", "Set count winners").setName("count"));

            optionsStart.add(new OptionData(STRING, "duration", "Examples: 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style. Preferably immediately in UTC ±0").setName("duration"));

            optionsStart.add(new OptionData(CHANNEL, "channel", "#text channel name").setName("channel"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mentioning a specific Role").setName("mention"));

            optionsStart.add(new OptionData(STRING, "role", "Giveaway is only for a specific role? Don't forget to specify the Role in the previous choice.")
                    .addChoice("yes", "yes")
                    .setName("role"));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Your Giveaway Image").setName("image"));

            optionsStop.add(new OptionData(STRING, "stop", "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1").setName("stop"));


            commands.addCommands(Commands.slash("language", "Setting language").addOptions(optionsLanguage));
            commands.addCommands(Commands.slash("start", "Create giveaway").addOptions(optionsStart));
            commands.addCommands(Commands.slash("stop", "Stop the Giveaway").addOptions(optionsStop));
            commands.addCommands(Commands.slash("help", "Bot commands"));
            commands.addCommands(Commands.slash("list", "List of participants"));
            commands.addCommands(Commands.slash("patreon", "Support us on Patreon"));
            commands.addCommands(Commands.slash("participants", "Get file with all participants by ID Giveaway").addOptions(option));


            commands.queue();

            System.out.println("Готово");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 12000)
    public void StopGiveaway() {
        try {
            executorService = Executors.newFixedThreadPool(2);
            int count = queue.size();

            if (!queue.isEmpty()) {
                for (int i = 0; i < count; i++) {
                    Giveaway giveaway = queue.poll();
                    if (giveaway != null) {
                        executorService.submit(new StopGiveawayByTimer(giveaway));
                    }
                }
                executorService.shutdown();
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private void setLanguages() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (int i = 0; i < listLanguages.size(); i++) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguages.get(i) + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);

                for (Object o : jsonObject.keySet()) {
                    String key = (String) o;

                    if (listLanguages.get(i).equals("rus")) {
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

    private void getPrefixFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM prefixs";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapPrefix.put(rs.getString("server_id"), rs.getString("prefix"));
            }

            rs.close();
            statement.close();
            connection.close();
            System.out.println("getPrefixFromDB()");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getMessageIdFromDB() {
        try {
            Connection connection = DriverManager.getConnection(URL_CONNECTION, USER_CONNECTION, PASSWORD_CONNECTION);

            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM active_giveaways";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {

                long guild_long_id = rs.getLong("guild_long_id");
                String channel_long_id = rs.getString("channel_id_long");
                String count_winners = rs.getString("count_winners");
                long message_id_long = rs.getLong("message_id_long");
                String giveaway_title = rs.getString("giveaway_title");
                Timestamp date_end_giveaway = rs.getTimestamp("date_end_giveaway");
                Long role_id_long = rs.getLong("role_id_long");
                Boolean is_for_specific_role = rs.getBoolean("is_for_specific_role");
                String url_image = rs.getString("url_image");
                String id_user_who_create_giveaway = rs.getString("id_user_who_create_giveaway");

                guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(guild_long_id));
                GiveawayRegistry.getInstance().putGift(
                        guild_long_id,
                        new Gift(guild_long_id,
                                Long.parseLong(channel_long_id),
                                activeGiveawayRepository,
                                participantsRepository));
                GiveawayRegistry.getInstance().getGift(guild_long_id).autoInsert();
                GiveawayRegistry.getInstance().putMessageId(guild_long_id, String.valueOf(message_id_long));
                GiveawayRegistry.getInstance().putIdMessagesWithGiveawayButtons(guild_long_id, String.valueOf(message_id_long));
                GiveawayRegistry.getInstance().putTitle(guild_long_id, giveaway_title);
                GiveawayRegistry.getInstance().putEndGiveawayDate(guild_long_id, date_end_giveaway);
                GiveawayRegistry.getInstance().putChannelId(guild_long_id, channel_long_id);
                GiveawayRegistry.getInstance().putCountWinners(guild_long_id, count_winners);
                GiveawayRegistry.getInstance().putRoleId(guild_long_id, role_id_long);
                GiveawayRegistry.getInstance().putIsForSpecificRole(guild_long_id, is_for_specific_role);
                GiveawayRegistry.getInstance().putUrlImage(guild_long_id, url_image);
                GiveawayRegistry.getInstance().putIdUserWhoCreateGiveaway(guild_long_id, id_user_who_create_giveaway);

                if (date_end_giveaway != null) {
                    queue.add(new Giveaway(guild_long_id, date_end_giveaway));
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

    private void getUsersWhoTakePartFromDB() {
        try {
            System.out.println("Получаем данные с БД и добавляем их в коллекции и экземпляры классов");

            for (int i = 1; i <= guildIdHashList.size(); i++) {

                List<Participants> participantsList = participantsRepository.getParticipantsByGuildIdLong(Long.valueOf(guildIdHashList.get(i)));

                for (int j = 0; j < participantsList.size(); j++) {

                    long userIdLong = participantsList.get(j).getUserIdLong();

//                    System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

                    //Добавляем пользователей в hashmap
                    GiveawayRegistry.getInstance()
                            .getGift(Long.parseLong(guildIdHashList.get(i))).getListUsersHash()
                            .put(String.valueOf(userIdLong), String.valueOf(userIdLong));

                    //Считаем пользователей в hashmap и устанавливаем верное значение
                    GiveawayRegistry.getInstance()
                            .getGift(Long.parseLong(guildIdHashList.get(i))).getListUsers()
                            .add(String.valueOf(userIdLong));

                    //Устанавливаем счетчик на верное число
                    GiveawayRegistry.getInstance()
                            .getGift(Long.parseLong(guildIdHashList.get(i)))
                            .setCount(participantsList.size());


                    GiveawayRegistry.getInstance()
                            .getGift(Long.parseLong(guildIdHashList.get(i))).getParticipantsJSON()
                            .add(new ParticipantsPOJO(
                                    String.valueOf(participantsList.get(j).getActiveGiveaways().getIdUserWhoCreateGiveaway()),
                                    String.valueOf(participantsList.get(j).getActiveGiveaways().getGuildLongId() + participantsList.get(j).getActiveGiveaways().getMessageIdLong()),
                                    participantsList.get(j).getActiveGiveaways().getGuildLongId(),
                                    participantsList.get(j).getUserIdLong(),
                                    participantsList.get(j).getNickName(),
                                    participantsList.get(j).getNickNameTag()));
                }
            }
            System.out.println("getUsersWhoTakePartFromDB()");
        } catch (Exception e) {
            e.printStackTrace();
        }
        guildIdHashList.clear();
    }

    @Scheduled(fixedDelay = 20000L)
    private void topGGAndStatcord() {
        if (!Config.isIsDev()) {
            try {
                DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                        .token(Config.getTopGgApiToken())
                        .botId(Config.getBotId())
                        .build();
                int serverCount = BotStartConfig.jda.getGuilds().size();

                TOP_GG_API.setStats(serverCount);
                BotStartConfig.jda.getPresence().setActivity(Activity.playing(BotStartConfig.activity + serverCount + " guilds"));
                IOUtils.toString(new URL("http://193.163.203.77:3001/api/push/SHAtSCMYvd?msg=OK&ping="), StandardCharsets.UTF_8);

                AtomicInteger usersCount = new AtomicInteger();
                BotStartConfig.jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.boticord.top/v1/stats"))
                        .POST(HttpRequest.BodyPublishers.ofString(new StatsPOJO(serverCount, 1, usersCount.get()).toString()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", System.getenv("BOTICORD"))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (!isLaunched) {
                    Statcord.start(
                            BotStartConfig.jda.getSelfUser().getId(),
                            Config.getStatcord(),
                            BotStartConfig.jda,
                            true,
                            3);
                    isLaunched = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public static Map<String, String> getMapPrefix() {
        return mapPrefix;
    }

    public static Map<String, String> getMapLanguages() {
        return mapLanguages;
    }

    public static JDA getJda() {
        return jda;
    }

    public static Deque<Giveaway> getQueue() {
        return queue;
    }

}
