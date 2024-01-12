package main.giveaway;

import main.giveaway.utils.GiveawayUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class GiveawayTimeHandler {

    public Timestamp updateTime(Giveaway giveaway, final String time) {
        Timestamp endGiveawayDate = giveaway.getEndGiveawayDate();
        if (time == null) return endGiveawayDate;
        ZoneOffset offset = ZoneOffset.UTC;
        LocalDateTime localDateTime;

        if (time.matches(GiveawayUtils.ISO_TIME_REGEX)) {
            localDateTime = LocalDateTime.parse(time, GiveawayUtils.FORMATTER);
        } else {
            long seconds = GiveawayUtils.getSeconds(time);
            localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(seconds);
        }

        long toEpochSecond = localDateTime.toEpochSecond(offset);
        int ONE_SECOND = 1000;
        Timestamp timestamp = new Timestamp(toEpochSecond * ONE_SECOND);
        giveaway.setEndGiveawayDate(timestamp);

        return timestamp;
    }
}