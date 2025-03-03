package main.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import main.jsonparser.ParserClass;
import main.model.entity.Scheduling;
import main.model.entity.Settings;
import main.model.repository.SchedulingRepository;
import main.model.repository.SettingsRepository;
import main.service.GiveawayUpdateListUser;
import main.service.ScheduleStartService;
import main.service.SlashService;
import main.service.UploadGiveawaysService;
import main.threads.StopGiveawayHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class BotStart {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotStart.class.getName());

    public static final String activity = "/start | ";
    //String - guildLongId
    private static final ConcurrentMap<Long, Settings> mapLanguages = new ConcurrentHashMap<>();

    @Getter
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //REPOSITORY
    private final UpdateController updateController;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;

    private final GiveawayUpdateListUser updateGiveawayByGuild;

    //Service
    private final SlashService slashService;
    private final ScheduleStartService scheduleStartService;
    private final UploadGiveawaysService uploadGiveawaysService;

    @Autowired
    public BotStart(UpdateController updateController,
                    SchedulingRepository schedulingRepository,
                    SettingsRepository settingsRepository,
                    GiveawayUpdateListUser updateGiveawayByGuild,
                    SlashService slashService,
                    ScheduleStartService scheduleStartService,
                    UploadGiveawaysService uploadGiveawaysService) {
        this.updateController = updateController;
        this.schedulingRepository = schedulingRepository;
        this.settingsRepository = settingsRepository;
        this.updateGiveawayByGuild = updateGiveawayByGuild;
        this.slashService = slashService;
        this.scheduleStartService = scheduleStartService;
        this.uploadGiveawaysService = uploadGiveawaysService;
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
            uploadGiveawaysService.uploadGiveaways(updateController);

            //Обновить команды
            slashService.updateSlash(jda);

            System.out.println("DevMode: " + Config.isIsDev() + " Time Build: " + "20:22");
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

    @Scheduled(fixedDelay = 120, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void updateActivity() {
        if (!Config.isIsDev()) {
            int serverCount = BotStart.jda.getGuilds().size();
            BotStart.jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));
        } else {
            BotStart.jda.getPresence().setActivity(Activity.playing("Develop"));
        }
    }

    @Scheduled(fixedDelay = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void scheduleStartGiveaway() {
        try {
            scheduleStartService.scheduleStart(updateController, jda);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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
