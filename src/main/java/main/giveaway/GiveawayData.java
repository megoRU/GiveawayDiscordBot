package main.giveaway;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class GiveawayData {

    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    private Timestamp endGiveawayDate;
    private int minParticipants = 2;

    public GiveawayData(long messageId,
                        int countWinners,
                        Long roleId,
                        boolean isForSpecificRole,
                        String urlImage,
                        String title,
                        Timestamp endGiveawayDate,
                        int minParticipants) {
        this.messageId = messageId;
        this.countWinners = countWinners;
        this.roleId = roleId;
        this.isForSpecificRole = isForSpecificRole;
        this.urlImage = urlImage;
        this.title = title;
        this.endGiveawayDate = endGiveawayDate;
        this.minParticipants = minParticipants;
    }

    public void setMinParticipants(int minParticipants) {
        if (minParticipants == 0) this.minParticipants = 2;
        else this.minParticipants = minParticipants;
    }

    public void setTitle(String title) {
        if (title == null) this.title = "Giveaway";
        else this.title = title;
    }
}