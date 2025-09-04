package main.giveaway;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.config.BotStart;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class GiveawayData {

    private final Set<Long> participantsList = ConcurrentHashMap.newKeySet();
    //messageId
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<ParticipantDTO>> queueConcurrentHashMap = new ConcurrentHashMap<>();
    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    private Timestamp endGiveawayDate;
    private int minParticipants = 1;
    private long userIdLong;

    public GiveawayData(long messageId,
                        int countWinners,
                        Long roleId,
                        Boolean isForSpecificRole,
                        String urlImage,
                        String title,
                        Timestamp endGiveawayDate,
                        int minParticipants,
                        long userIdLong) {
        this.messageId = messageId;
        this.countWinners = countWinners;
        this.roleId = roleId;
        this.isForSpecificRole = Optional.ofNullable(isForSpecificRole).orElse(false);
        this.urlImage = urlImage;
        this.title = title;

        if (endGiveawayDate == null) {
            String zonesIdByUser = BotStart.getZonesIdByUser(userIdLong);
            ZoneId zoneId = ZoneId.of(zonesIdByUser);
            endGiveawayDate = Timestamp.from(Instant.now().atZone(zoneId).toInstant().plus(30, ChronoUnit.DAYS));
        }
        this.endGiveawayDate = endGiveawayDate;
        this.minParticipants = minParticipants;
        this.userIdLong = userIdLong;
    }

    public void addUserToQueue(User user) {
        String name = user.getName();
        long userIdLong = user.getIdLong();
        ParticipantDTO participantDTO = new ParticipantDTO(userIdLong, name);

        ConcurrentLinkedQueue<ParticipantDTO> users = queueConcurrentHashMap.get(messageId);
        if (users == null) {
            users = new ConcurrentLinkedQueue<>();
            users.add(participantDTO);
        } else {
            users.add(participantDTO);
        }
        queueConcurrentHashMap.put(messageId, users);
    }

    @Nullable
    public ConcurrentLinkedQueue<ParticipantDTO> getCollectionQueue() {
        return queueConcurrentHashMap.get(messageId);
    }

    public boolean participantContains(Long user) {
        return participantsList.contains(user);
    }

    public int getParticipantSize() {
        return participantsList.size();
    }

    public void addParticipant(Long userId) {
        participantsList.add(userId);
    }

    public void setParticipantsList(Set<Long> participantsMap) {
        participantsList.addAll(participantsMap);
    }

    public void setMinParticipants(int minParticipants) {
        if (minParticipants == 0) this.minParticipants = 1;
        else this.minParticipants = minParticipants;
    }

    public void setTitle(String title) {
        if (title == null) this.title = "Giveaway";
        else this.title = title;
    }
}