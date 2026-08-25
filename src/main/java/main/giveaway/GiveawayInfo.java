package main.giveaway;

import java.time.Instant;

public record GiveawayInfo(String title, int countWinners, int minParticipants, Instant endGiveawayDate, long userIdLong) {

}
