package main.giveaway;

import main.giveaway.utils.GiveawayUtils;
import main.threads.StopGiveawayByTimer;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Timer;

public class GiveawayTimeHandler {

    public Timestamp updateTime(Giveaway giveaway, final String time) {
        Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
        if (time == null) return endGiveawayDate;
        long guildId = giveaway.getGuildId();
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        } else {
            long seconds = GiveawayUtils.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
        }

        long toEpochSecond = localDateTime.toEpochSecond(offset);
        Timestamp timestamp = new Timestamp(toEpochSecond * 1000);
        giveaway.setEndGiveawayDate(timestamp);

        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.cancelGiveawayTimer(guildId);

        Timer timer = new Timer();
        StopGiveawayByTimer stopGiveawayByTimer = new StopGiveawayByTimer(guildId);
        Date date = new Date(timestamp.getTime());

        timer.schedule(stopGiveawayByTimer, date);
        instance.putGiveawayTimer(guildId, stopGiveawayByTimer, timer);
        return timestamp;
    }
}