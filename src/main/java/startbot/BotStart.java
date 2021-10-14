package startbot;

import config.Config;
import db.DataBase;
import events.MessageWhenBotJoinToGuild;
import giveaway.*;
import jsonparser.JSONParsers;
import jsonparser.ParserClass;
import messagesevents.LanguageChange;
import messagesevents.MessageInfoHelp;
import messagesevents.PrefixChange;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import threads.Giveaway;
import threads.TopGGAndStatcordThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BotStart {

    public static final String activity = "!help | ";
    public static final String version = "v15 ";
    private static final Deque<Giveaway> queue = new ArrayDeque<>();
    private static final Map<String, String> mapPrefix = new HashMap<>();
    private static final ConcurrentMap<String, String> mapLanguages = new ConcurrentHashMap<>();
    private static final Map<Integer, String> guildIdHashList = new HashMap<>();
    private static JDA jda;
    private final JSONParsers jsonParsers = new JSONParsers();
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

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

    public void startBot() throws Exception {
        //Получаем языки
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
        jdaBuilder.setActivity(Activity.playing(activity + TopGGAndStatcordThread.serverCount + " guilds"));
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
//        jda.updateCommands().queue();
//
//
//        List<OptionData> optionsLanguage = new ArrayList<>();
//        List<OptionData> optionsStart = new ArrayList<>();
//        List<OptionData> optionsStop = new ArrayList<>();
//
//        optionsLanguage.add(new OptionData(OptionType.STRING, "bot", "Setting the bot language")
//                .addChoice("eng", "eng")
//                .addChoice("rus", "rus")
//                .setRequired(true));
//
//        optionsStart.add(new OptionData(OptionType.STRING, "title", "Title for Giveaway")
//                .setName("title")
//        );
//
//        optionsStart.add(new OptionData(OptionType.INTEGER, "count", "Set count winners")
//                .setName("count")
//        );
//
//        optionsStart.add(new OptionData(OptionType.STRING, "duration", "Examples: 20m, 10h, 1d. You can not specify the time, then it will be infinite")
//                .setName("duration")
//        );
//
//        optionsStart.add(new OptionData(OptionType.CHANNEL, "channel", "#text channel name")
//                .setName("channel")
//        );
//
//        optionsStop.add(new OptionData(OptionType.STRING, "stop", "Examples: 1, 2... If not specified, it will end with the specified at creation or with the default 1")
//                .setName("stop")
//        );
//
//
//        jda.upsertCommand("language", "Setting language").addOptions(optionsLanguage).queue();
//        jda.upsertCommand("giveaway-start", "Create giveaway").addOptions(optionsStart).queue();
//        jda.upsertCommand("giveaway-stop", "Stop the Giveaway").addOptions(optionsStop).queue();


    }

    private void getMessageIdFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM ActiveGiveaways";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {

                long guild_long_id = rs.getLong("guild_long_id");
                String channel_long_id = rs.getString("channel_id_long");
                String count_winners = rs.getString("count_winners");
                long message_id_long = rs.getLong("message_id_long");
                String giveaway_title = rs.getString("giveaway_title");
                String date_end_giveaway = rs.getString("date_end_giveaway");

                guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(guild_long_id));
                GiveawayRegistry.getInstance().setGift(guild_long_id, new Gift(guild_long_id, Long.parseLong(channel_long_id)));
                GiveawayRegistry.getInstance().getActiveGiveaways().get(guild_long_id).autoInsert();
                GiveawayRegistry.getInstance().getMessageId().put(guild_long_id, String.valueOf(message_id_long));
                GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild_long_id, String.valueOf(message_id_long));
                GiveawayRegistry.getInstance().getTitle().put(guild_long_id, giveaway_title);
                GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild_long_id, date_end_giveaway == null ? "null" : date_end_giveaway);
                GiveawayRegistry.getInstance().getChannelId().put(guild_long_id, channel_long_id);
                GiveawayRegistry.getInstance().getCountWinners().put(guild_long_id, count_winners);

                //Добавляем кнопки для Giveaway в Gift class
                setButtonsInGift(String.valueOf(guild_long_id), guild_long_id);

                if (date_end_giveaway != null) {
                    queue.add(new Giveaway(guild_long_id, date_end_giveaway));
                }
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getUsersWhoTakePartFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            System.out.println("Получаем данные с БД и добавляем их в коллекции и экземпляры классов");

            for (int i = 1; i <= guildIdHashList.size(); i++) {
                String sql = "SELECT * FROM `" + guildIdHashList.get(i) + "`;";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {

                    long userIdLong = rs.getLong("user_long_id");

                    //System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

                    //Добавляем пользователей в hashmap
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash()
                            .put(String.valueOf(userIdLong), String.valueOf(userIdLong));

                    //Считаем пользователей в hashmap и устанавливаем верное значение
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(Long.parseLong(guildIdHashList.get(i))).getListUsers()
                            .add(String.valueOf(userIdLong));

                    //Устанавливаем счетчик на верное число
                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways()
                            .get(Long.parseLong(guildIdHashList.get(i))).setCount(GiveawayRegistry.getInstance()
                                    .getActiveGiveaways()
                                    .get(Long.parseLong(guildIdHashList.get(i))).getListUsersHash().size());
                }
                rs.close();
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        guildIdHashList.clear();
    }

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

    private void getPrefixFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM prefixs";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapPrefix.put(rs.getString("serverId"), rs.getString("prefix"));
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getLocalizationFromDB() {
        try {
            Statement statement = DataBase.getConnection().createStatement();
            String sql = "SELECT * FROM language";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                mapLanguages.put(rs.getString("serverId"), rs.getString("lang"));
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setLanguages() throws IOException, ParseException {

        List<String> listLanguages = new ArrayList<>();
        listLanguages.add("rus");
        listLanguages.add("eng");

        for (int i = 0; i < listLanguages.size(); i++) {
            InputStream inputStream = getClass().getResourceAsStream("/json/" + listLanguages.get(i) + ".json");
            assert inputStream != null;
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
    }
}