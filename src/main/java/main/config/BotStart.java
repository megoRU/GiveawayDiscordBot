package main.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.giveaway.Giveaway;
import main.jsonparser.ParserClass;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.entity.Scheduling;
import main.model.entity.Settings;
import main.model.entity.UserZoneId;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.SchedulingRepository;
import main.model.repository.SettingsRepository;
import main.model.repository.UserZoneIdRepository;
import main.service.GiveawayRepositoryService;
import main.service.SaveUsersService;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class BotStart {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotStart.class.getName());

    public static final String activity = "/start | ";
    //String - guildLongId
    private static final ConcurrentMap<Long, Settings> mapLanguages = new ConcurrentHashMap<>();
    //Long - userId
    private static final ConcurrentMap<Long, String> mapZonesId = new ConcurrentHashMap<>();

    @Getter
    private static JDA jda;
    private final JDABuilder jdaBuilder = JDABuilder.createDefault(Config.getTOKEN());

    //COMPONENT
    private final UpdateController updateController;

    //REPOSITORY
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final SchedulingRepository schedulingRepository;
    private final SettingsRepository settingsRepository;
    private final UserZoneIdRepository userZoneIdRepository;

    //Service
    private final GiveawayRepositoryService giveawayRepositoryService;
    private final SlashService slashService;
    private final ScheduleStartService scheduleStartService;
    private final UploadGiveawaysService uploadGiveawaysService;
    private final SaveUsersService saveUsersService;
    private final CoreBot coreBot;

    @PostConstruct
    public void startBot() {
        try {
            //Устанавливаем языки
            setLanguages();
            getLocalizationFromDB();
            getUserZoneIdFromDB();

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
            jdaBuilder.addEventListeners(coreBot);

            if (Config.IS_PROXY) {
                System.setProperty("socksProxyHost", Config.PROXY_IP);
                System.setProperty("socksProxyPort", "10808");
            }

            jda = jdaBuilder.build();
            jda.awaitReady();

            //Получаем Giveaway и пользователей. Устанавливаем данные
            uploadGiveawaysService.uploadGiveaways(updateController);

            //Обновить команды
            slashService.updateSlash(jda);

            updateActivity();

            System.out.println("DevMode: " + Config.isIsDev() + " Time Build: " + "11:29");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 60, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    private void saveUsers() {
        try {
            saveUsersService.saveParticipants();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void updateActivity() {
        if (!Config.isIsDev()) {
            int serverCount = BotStart.jda.getGuilds().size();
            BotStart.jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));
        } else {
            BotStart.jda.getPresence().setActivity(Activity.playing("Develop"));
        }
    }

    @Scheduled(fixedDelay = 60, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
    private void schStartGiveaway() {
        try {
            List<Scheduling> schedulingList = schedulingRepository.findAll();

            for (Scheduling scheduling : schedulingList) {
                String idSalt = scheduling.getIdSalt();

                Long createdUserId = scheduling.getCreatedUserId();
                String zonesIdByUser = BotStart.getZonesIdByUser(createdUserId);

                ZoneId offset = ZoneId.of(zonesIdByUser);
                ZonedDateTime odt = Instant.now().atZone(offset);
                Instant instant = odt.toInstant();

                Instant dateCreateGiveaway = scheduling.getDateCreateGiveaway();

                if (!instant.isAfter(dateCreateGiveaway)) {
                    continue;
                }

                scheduleStartService.scheduleStart(idSalt, updateController, jda);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 60, initialDelay = 60, timeUnit = TimeUnit.SECONDS)
    private void stopGiveawayTimer() {
        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll();

        for (ActiveGiveaways activeGiveaways : activeGiveawaysList) {
            try {
                Set<Long> participantsList = activeGiveaways.getParticipants() != null ? activeGiveaways.getParticipants()
                        .stream()
                        .map(Participants::getUserId)
                        .collect(Collectors.toSet()) : Collections.emptySet();

                Giveaway giveaway = new Giveaway(
                        activeGiveaways.getGuildId(),
                        activeGiveaways.getChannelId(),
                        activeGiveaways.getCreatedUserId(),
                        activeGiveaways.isFinish(),
                        false,
                        activeGiveaways.getMessageId(),
                        activeGiveaways.getCountWinners(),
                        activeGiveaways.getRoleId(),
                        activeGiveaways.getIsForSpecificRole(),
                        activeGiveaways.getUrlImage(),
                        activeGiveaways.getTitle(),
                        activeGiveaways.getEndGiveawayDate(),
                        activeGiveaways.getMinParticipants() == null ? 1 : activeGiveaways.getMinParticipants(),
                        giveawayRepositoryService,
                        updateController
                );
                giveaway.setParticipantsList(participantsList);

                StopGiveawayHandler stopGiveawayHandler = new StopGiveawayHandler();
                stopGiveawayHandler.handleGiveaway(giveaway);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
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
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
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

    private void getUserZoneIdFromDB() {
        try {
            List<UserZoneId> userZoneIdList = userZoneIdRepository.findAll();
            for (UserZoneId userZoneId : userZoneIdList) {
                mapZonesId.put(userZoneId.getUserId(), userZoneId.getZoneId());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Map<Long, Settings> getMapLanguages() {
        return mapLanguages;
    }

    public static void setTimeZone(long userId, String timeZone) {
        mapZonesId.put(userId, timeZone);
    }

    public static String getZonesIdByUser(long userId) {
        String zoneId = mapZonesId.get(userId);
        return Objects.requireNonNullElse(zoneId, "UTC");
    }
}