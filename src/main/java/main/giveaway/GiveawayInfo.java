package main.giveaway;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class GiveawayInfo {

    private final String title;
    private final int countWinners;
    private final int minParticipants;
    private final Instant endGiveawayDate;
    private final long userIdLong;
}
