package main.service;

import main.config.BotStart;
import main.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.boticordjava.api.entity.bot.stats.BotStats;
import org.boticordjava.api.impl.BotiCordAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BotStatisticsService {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotStatisticsService.class.getName());
    //API
    private final BotiCordAPI api = new BotiCordAPI.Builder().token(Config.getBoticord()).build();

    public void updateStatistics(boolean isDev, JDA jda) {
        if (!isDev) {
            try {
                int serverCount = jda.getGuilds().size();
                jda.getPresence().setActivity(Activity.playing(BotStart.activity + serverCount + " guilds"));

                //BOTICORD API
                AtomicInteger usersCount = new AtomicInteger();
                jda.getGuilds().forEach(g -> usersCount.addAndGet(g.getMembers().size()));

                BotStats botStats = new BotStats(usersCount.get(), serverCount, 1);
                api.setBotStats(Config.getBotId(), botStats);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}