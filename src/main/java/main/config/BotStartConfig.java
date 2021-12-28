package main.config;

import main.events.MessageWhenBotJoinToGuild;
import main.giveaway.GiveawayRegistry;
import main.giveaway.MessageGift;
import main.giveaway.ReactionsButton;
import main.giveaway.SlashCommand;
import main.jsonparser.JSONParsers;
import main.jsonparser.ParserClass;
import main.messagesevents.LanguageChange;
import main.messagesevents.MessageInfoHelp;
import main.messagesevents.PrefixChange;
import main.startbot.Statcord;
import main.threads.Giveaway;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class BotStartConfig {

    public static final String activity = "!help | ";
    public static final String version = "v15 ";
    private static final Deque<Giveaway> queue = new ArrayDeque<>();
    //String - guildLongId
    private static final Map<String, String> mapPrefix = new HashMap<>();
    //String - guildLongId
    private static final ConcurrentMap<String, String> mapLanguages = new ConcurrentHashMap<>();
    private static final Map<Integer, String> guildIdHashList = new HashMap<>();
    private static JDA jda;
    private final JSONParsers jsonParsers = new JSONParsers();
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());
    public static ExecutorService executorService;
    public static int serverCount;
    private volatile boolean isLaunched;

    //REPOSITORY


    @Autowired
    public BotStartConfig() {

    }

    @Bean
    public void startBot() {
        try {
            //Загружаем GiveawayRegistry
            GiveawayRegistry.getInstance();


            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing(activity + serverCount + " guilds"));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild());
            jdaBuilder.addEventListeners(new MessageGift());
            jdaBuilder.addEventListeners(new PrefixChange());
            jdaBuilder.addEventListeners(new MessageInfoHelp());
            jdaBuilder.addEventListeners(new LanguageChange());
            jdaBuilder.addEventListeners(new ReactionsButton());
            jdaBuilder.addEventListeners(new SlashCommand());


            jda = jdaBuilder.build();
            jda.awaitReady();

            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Обновить команды
        //updateSlashCommands();
        System.out.println("18:42");
    }

    @Bean
    public void StopGiveaway(){
        Thread thread = new Thread(() -> {
            try {
                while (true) {
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
                        Thread.sleep(2000);
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void updateSlashCommands() {
        try {
            jda.updateCommands().queue();

            Thread.sleep(4000L);

            List<OptionData> optionsLanguage = new ArrayList<>();
            List<OptionData> optionsStart = new ArrayList<>();
            List<OptionData> optionsStop = new ArrayList<>();

            optionsLanguage.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
                    .addChoice("eng", "eng")
                    .addChoice("rus", "rus")
                    .setRequired(true));

            optionsStart.add(new OptionData(OptionType.STRING, "title", "Title for Giveaway")
                    .setName("title")
            );

            optionsStart.add(new OptionData(OptionType.INTEGER, "count", "Set count winners")
                    .setName("count")
            );

            optionsStart.add(new OptionData(OptionType.STRING, "duration", "Examples: 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style. Preferably immediately in UTC ±0")
                    .setName("duration")
            );

            optionsStart.add(new OptionData(OptionType.CHANNEL, "channel", "#text channel name")
                    .setName("channel")
            );

            optionsStop.add(new OptionData(OptionType.STRING, "stop", "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1")
                    .setName("stop")
            );

            jda.upsertCommand("language", "Setting language").addOptions(optionsLanguage).queue();
            jda.upsertCommand("giveaway-start", "Create giveaway").addOptions(optionsStart).queue();
            jda.upsertCommand("giveaway-stop", "Stop the Giveaway").addOptions(optionsStop).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void setLanguages() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Bean
//    public void getPrefixFromDB() {
//        try {
//            for (int i = 0; i < prefixRepository.getPrefix().size(); i++) {
//                mapPrefix.put(
//                        prefixRepository.getPrefix().get(i).getServerId(),
//                        prefixRepository.getPrefix().get(i).getPrefix());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void setButtonsInGift(String guildId, long longGuildId) {
        if (getMapLanguages().get(guildId) != null) {
            if (getMapLanguages().get(guildId).equals("rus")) {
                GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                        .add(Button.success(longGuildId + ":" + ReactionsButton.PRESENT,
                                jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀ ⠀⠀"));
            } else {
                GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                        .add(Button.success(longGuildId + ":" + ReactionsButton.PRESENT,
                                jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀⠀⠀⠀⠀⠀⠀⠀"));
            }
        } else {
            GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                    .add(Button.success(longGuildId + ":" + ReactionsButton.PRESENT,
                            jsonParsers.getLocale("gift_Press_Me_Button", guildId) + "⠀⠀⠀⠀⠀⠀⠀⠀"));
        }

        GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                .add(Button.danger(longGuildId + ":" + ReactionsButton.STOP_ONE,
                        jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "1")));

        GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                .add(Button.danger(longGuildId + ":" + ReactionsButton.STOP_TWO,
                        jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "2")));

        GiveawayRegistry.getInstance().getActiveGiveaways().get(longGuildId).getButtons()
                .add(Button.danger(longGuildId + ":" + ReactionsButton.STOP_THREE,
                        jsonParsers.getLocale("gift_Stop_Button", guildId).replaceAll("\\{0}", "3")));
    }

//    @Bean
//    public void getMessageIdFromDB() {
//        try {
//            Statement statement = DataBase.getConnection().createStatement();
//            String sql = "SELECT * FROM ActiveGiveaways";
//            ResultSet rs = statement.executeQuery(sql);
//            while (rs.next()) {
//
//                long guild_long_id = rs.getLong("guild_long_id");
//                String channel_long_id = rs.getString("channel_id_long");
//                String count_winners = rs.getString("count_winners");
//                long message_id_long = rs.getLong("message_id_long");
//                String giveaway_title = rs.getString("giveaway_title");
//                String date_end_giveaway = rs.getString("date_end_giveaway");
//
//                guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(guild_long_id));
//                GiveawayRegistry.getInstance().setGift(guild_long_id, new Gift(guild_long_id, Long.parseLong(channel_long_id)));
//                GiveawayRegistry.getInstance().getActiveGiveaways().get(guild_long_id).autoInsert();
//                GiveawayRegistry.getInstance().getMessageId().put(guild_long_id, String.valueOf(message_id_long));
//                GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild_long_id, String.valueOf(message_id_long));
//                GiveawayRegistry.getInstance().getTitle().put(guild_long_id, giveaway_title);
//                GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild_long_id, date_end_giveaway == null ? "null" : date_end_giveaway);
//                GiveawayRegistry.getInstance().getChannelId().put(guild_long_id, channel_long_id);
//                GiveawayRegistry.getInstance().getCountWinners().put(guild_long_id, count_winners);
//
//                //Добавляем кнопки для Giveaway в Gift class
//                setButtonsInGift(String.valueOf(guild_long_id), guild_long_id);
//
//                if (date_end_giveaway != null) {
//                    queue.add(new Giveaway(guild_long_id, date_end_giveaway));
//                }
//            }
//            rs.close();
//            statement.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    @Scheduled(fixedDelay = 20000L)
    private void topGGAndStatcord() {
        try {
            //Заодно проверяем коннект, чтобы не было потом проблем
            DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
                    .token(Config.getTopGgApiToken())
                    .botId(Config.getBotId())
                    .build();
            serverCount = (int) jda.getGuildCache().size();
            TOP_GG_API.setStats(serverCount);
            jda.getPresence().setActivity(Activity.playing(activity + serverCount + " guilds"));

            if (!isLaunched) {
                Statcord.start(
                        jda.getSelfUser().getId(),
                        Config.getStatcord(),
                        jda,
                        true,
                        3);
                isLaunched = true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Bean
//    public void getUsersWhoTakePartFromDB() {
//        try {
//            Statement statement = DataBase.getConnection().createStatement();
//            System.out.println("Получаем данные с БД и добавляем их в коллекции и экземпляры классов");
//
//            for (int i = 1; i <= guildIdHashList.size(); i++) {
//                String sql = "SELECT * FROM `" + guildIdHashList.get(i) + "`;";
//                ResultSet rs = statement.executeQuery(sql);
//                while (rs.next()) {
//
//                    long userIdLong = rs.getLong("user_long_id");
//
//                    //System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);
//
//                    //Добавляем пользователей в hashmap
//                    GiveawayRegistry.getInstance()
//                            .getActiveGiveaways()
//                            .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash()
//                            .put(String.valueOf(userIdLong), String.valueOf(userIdLong));
//
//                    //Считаем пользователей в hashmap и устанавливаем верное значение
//                    GiveawayRegistry.getInstance()
//                            .getActiveGiveaways()
//                            .get(Long.parseLong(guildIdHashList.get(i))).getListUsers()
//                            .add(String.valueOf(userIdLong));
//
//                    //Устанавливаем счетчик на верное число
//                    GiveawayRegistry.getInstance()
//                            .getActiveGiveaways()
//                            .get(Long.parseLong(guildIdHashList.get(i))).setCount(GiveawayRegistry.getInstance()
//                                    .getActiveGiveaways()
//                                    .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash().size());
//                }
//                rs.close();
//            }
//            statement.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        guildIdHashList.clear();
//    }

//    @Bean
//    public void getLocalizationFromDB() {
//        try {
//            for (int i = 0; i < languageRepository.getLanguages().size(); i++) {
//                mapLanguages.put(
//                        languageRepository.getLanguages().get(i).getUserIdLong(),
//                        languageRepository.getLanguages().get(i).getLanguage());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


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
