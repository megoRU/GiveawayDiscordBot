package main.config;

import main.events.MessageWhenBotJoinToGuild;
import main.giveaway.*;
import main.jsonparser.JSONParsers;
import main.jsonparser.ParserClass;
import main.messagesevents.LanguageChange;
import main.messagesevents.MessageInfoHelp;
import main.messagesevents.PrefixChange;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.LanguageRepository;
import main.model.repository.ParticipantsRepository;
import main.model.repository.PrefixRepository;
import main.threads.Giveaway;
import main.threads.StopGiveawayByTimer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.Button;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

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
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final LanguageRepository languageRepository;
    private final PrefixRepository prefixRepository;
    private final ParticipantsRepository participantsRepository;

    @Autowired
    public BotStartConfig(ActiveGiveawayRepository activeGiveawayRepository, LanguageRepository
            languageRepository, PrefixRepository prefixRepository, ParticipantsRepository participantsRepository) {
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

            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing(activity + serverCount + " guilds"));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new MessageWhenBotJoinToGuild(prefixRepository, activeGiveawayRepository, languageRepository));
            jdaBuilder.addEventListeners(new MessageGift(activeGiveawayRepository, participantsRepository));
            jdaBuilder.addEventListeners(new PrefixChange(prefixRepository));
            jdaBuilder.addEventListeners(new MessageInfoHelp());
            jdaBuilder.addEventListeners(new LanguageChange(languageRepository));
            jdaBuilder.addEventListeners(new ReactionsButton(languageRepository));
            jdaBuilder.addEventListeners(new SlashCommand(languageRepository, activeGiveawayRepository, participantsRepository));

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
    public void StopGiveaway() {
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
            jda.upsertCommand("start", "Create giveaway").addOptions(optionsStart).queue();
            jda.upsertCommand("stop", "Stop the Giveaway").addOptions(optionsStop).queue();
            jda.upsertCommand("help", "Bot commands").queue();

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

    @Bean
    public void getPrefixFromDB() {
        try {
            for (int i = 0; i < prefixRepository.getPrefixs().size(); i++) {
                mapPrefix.put(
                        prefixRepository.getPrefixs().get(i).getServerId(),
                        prefixRepository.getPrefixs().get(i).getPrefix());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Bean
    public void getMessageIdFromDB() {
        try {

            List<ActiveGiveaways> arl = activeGiveawayRepository.getAllActiveGiveaways();

            for (int i = 0; i < arl.size(); i++) {

                guildIdHashList.put(guildIdHashList.size() + 1, String.valueOf(arl.get(i).getGuildLongId()));

                GiveawayRegistry.getInstance().setGift(arl.get(i).getGuildLongId(), new Gift(
                        arl.get(i).getGuildLongId(),
                        arl.get(i).getChannelIdLong(),
                        activeGiveawayRepository,
                        participantsRepository));

                GiveawayRegistry.getInstance().getActiveGiveaways().get(arl.get(i).getGuildLongId()).autoInsert();

                GiveawayRegistry.getInstance().getMessageId().put(
                        arl.get(i).getGuildLongId(),
                        String.valueOf(arl.get(i).getMessageIdLong()));

                GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(
                        arl.get(i).getGuildLongId(),
                        String.valueOf(arl.get(i).getMessageIdLong()));

                GiveawayRegistry.getInstance().getTitle().put(
                        arl.get(i).getGuildLongId(),
                        arl.get(i).getGiveawayTitle());

                GiveawayRegistry.getInstance().getEndGiveawayDate().put(
                        arl.get(i).getGuildLongId(),
                        arl.get(i).getDateEndGiveaway() == null ? "null" : arl.get(i).getDateEndGiveaway());

                GiveawayRegistry.getInstance().getChannelId().put(
                        arl.get(i).getGuildLongId(),
                        String.valueOf(arl.get(i).getChannelIdLong()));

                GiveawayRegistry.getInstance().getCountWinners().put(
                        arl.get(i).getGuildLongId(),
                        arl.get(i).getCountWinners());

                //Добавляем кнопки для Giveaway в Gift class
                setButtonsInGift(String.valueOf(arl.get(i).getGuildLongId()), arl.get(i).getGuildLongId());

                if (arl.get(i).getDateEndGiveaway() != null) {
                    queue.add(new Giveaway(arl.get(i).getGuildLongId(), arl.get(i).getDateEndGiveaway()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public void getUsersWhoTakePartFromDB() {
        try {

            System.out.println("Получаем данные с БД и добавляем их в коллекции и экземпляры классов");


            for (int i = 1; i <= guildIdHashList.size(); i++) {

                List<Participants> participantsList = participantsRepository.getParticipantsByGuildIdLong(Long.valueOf(guildIdHashList.get(i)));

                for (int j = 0; j < participantsList.size(); j++) {

                    long userIdLong = participantsList.get(j).getUserIdLong();

                    System.out.println("Guild id: " + guildIdHashList.get(i) + " user id long: " + userIdLong);

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        guildIdHashList.clear();
    }

//    @Scheduled(fixedDelay = 20000L)
//    private void topGGAndStatcord() {
//        try {
//            System.out.println(Config.getTopGgApiToken());
//            DiscordBotListAPI TOP_GG_API = new DiscordBotListAPI.Builder()
//                    .token(Config.getTopGgApiToken())
//                    .botId(Config.getBotId())
//                    .build();
//            serverCount = jda.getGuilds().size();
//            TOP_GG_API.setStats(serverCount);
//            jda.getPresence().setActivity(Activity.playing(activity + serverCount + " guilds"));
//
//            if (!isLaunched) {
//                Statcord.start(
//                        jda.getSelfUser().getId(),
//                        Config.getStatcord(),
//                        jda,
//                        true,
//                        3);
//                isLaunched = true;
//            }
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @Bean
    public void getLocalizationFromDB() {
        try {
            for (int i = 0; i < languageRepository.getLanguages().size(); i++) {
                mapLanguages.put(
                        languageRepository.getLanguages().get(i).getServerId(),
                        languageRepository.getLanguages().get(i).getLanguage());
            }
        } catch (Exception e) {
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
