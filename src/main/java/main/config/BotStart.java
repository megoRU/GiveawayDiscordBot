package main.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import main.controller.UpdateController;
import main.core.CoreBot;
import main.model.entity.Settings;
import main.model.repository.SettingsRepository;
import main.service.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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

    //REPOSITORY
    private final UpdateController updateController;
    private final SettingsRepository settingsRepository;

    //Service
    private final ScheduleStartService scheduleStartService;
    private final StopGiveawayService stopGiveawayService;
    private final SavingParticipantsService savingParticipantsService;
    private final LanguageService languageService;
    private final BotStatisticsService botStatisticsService;
    private final ParticipantsUpdaterService participantsUpdaterService;
    private final GiveawayUpdaterService giveawayUpdaterService;
    private final UpdateSlashService updateSlashService;

    @PostConstruct
    private void startBot() {
        try {
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
            jdaBuilder.setMemberCachePolicy(MemberCachePolicy.ALL);
            jdaBuilder.enableIntents(intents);
            jdaBuilder.setAutoReconnect(true);
            jdaBuilder.setStatus(OnlineStatus.ONLINE);
            jdaBuilder.setActivity(Activity.playing("Starting..."));
            jdaBuilder.setBulkDeleteSplittingEnabled(false);
            jdaBuilder.addEventListeners(new CoreBot(updateController));

            jda = jdaBuilder.build();
            jda.awaitReady();

            //Загружаем активные Giveaways
            giveawayUpdaterService.updateGiveaway(jda);

            List<Command> complete = jda.retrieveCommands().complete();
            complete.forEach(command -> System.out.println(command.toString()));

            System.out.println("Режим тестирования: " + Config.isIsDev());

            //Обновить команды
            updateSlashService.updateSlash(jda);
            System.out.println("15:31");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @PostConstruct
    private void setLanguages() {
        languageService.languageParse();
    }

    @PostConstruct
    private void getLocalizationFromDB() {
        try {
            List<Settings> settingsList = settingsRepository.findAll();
            settingsList.forEach(settings -> mapLanguages.put(settings.getServerId(), settings));
            System.out.println("getLocalizationFromDB()");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 120, initialDelay = 8, timeUnit = TimeUnit.SECONDS)
    private void updateStatistics() {
        botStatisticsService.updateStatistics(Config.isIsDev(), jda);
    }

    @Scheduled(fixedDelay = 2, initialDelay = 2, timeUnit = TimeUnit.SECONDS)
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

    @Scheduled(fixedDelay = (60 * 2), initialDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void updateUserList() {
        participantsUpdaterService.update(jda);
    }

    public static Map<Long, Settings> getMapLanguages() {
        return mapLanguages;
    }
}