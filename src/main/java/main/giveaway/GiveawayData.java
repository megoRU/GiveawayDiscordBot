package main.giveaway;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class GiveawayData {

    private final ConcurrentHashMap<String, String> participantsList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<User>> queueConcurrentHashMap = new ConcurrentHashMap<>();
    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    private Timestamp endGiveawayDate;
    private int minParticipants = 1;

    public GiveawayData(long messageId,
                        int countWinners,
                        Long roleId,
                        Boolean isForSpecificRole,
                        String urlImage,
                        String title,
                        Timestamp endGiveawayDate,
                        int minParticipants) {
        this.messageId = messageId;
        this.countWinners = countWinners;
        this.roleId = roleId;
        this.isForSpecificRole = Optional.ofNullable(isForSpecificRole).orElse(false);
        this.urlImage = urlImage;
        this.title = title;
        this.endGiveawayDate = endGiveawayDate;
        this.minParticipants = minParticipants;
    }

    public void addUserToQueue(User user) {
        ConcurrentLinkedQueue<User> users = queueConcurrentHashMap.get(messageId);
        if (users == null) {
            users = new ConcurrentLinkedQueue<>();
            users.add(user);
        } else {
            users.add(user);
        }
        queueConcurrentHashMap.put(messageId, users);
    }

    @Nullable
    public ConcurrentLinkedQueue<User> getCollectionQueue() {
        return queueConcurrentHashMap.get(messageId);
    }

    public boolean participantContains(String user) {
        return participantsList.containsKey(user);
    }

    public int getParticipantSize() {
        return participantsList.size();
    }

    public void addParticipant(String userId) {
        participantsList.put(userId, userId);
    }

    public void setParticipantsList(Map<String, String> participantsMap) {
        participantsMap.forEach((userId, user) -> participantsList.put(userId, userId));
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