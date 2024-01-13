package main.giveaway;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import main.core.events.ReactionEvent;
import main.jsonparser.JSONParsers;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

@Service
@Getter
public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    //User LIST
    @Getter(AccessLevel.NONE)
    private Set<String> listUsersHash;

    //GiveawayData
    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    @Nullable
    @Setter
    private Timestamp endGiveawayDate;
    private int minParticipants = 2;
    private long guildId;
    private long textChannelId;
    private long userIdLong;
    private boolean finishGiveaway;
    @Setter
    private boolean lockEnd;

    //DTO
    @Getter(AccessLevel.NONE)
    private final ConcurrentLinkedQueue<Participants> participantsList;

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;

    //Service
    private final GiveawayMessageHandler giveawayMessageHandler;
    private final GiveawaySaving giveawaySaving;
    private final GiveawayEnd giveawayEnd;

    private final GiveawayTimeHandler giveawayTimeHandler;

    @Autowired
    public Giveaway(ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository,
                    GiveawayMessageHandler giveawayMessageHandler,
                    GiveawaySaving giveawaySaving,
                    GiveawayEnd giveawayEnd) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.giveawayMessageHandler = giveawayMessageHandler;
        this.giveawaySaving = giveawaySaving;
        this.giveawayEnd = giveawayEnd;

        this.giveawayTimeHandler = new GiveawayTimeHandler();
        this.listUsersHash = new HashSet<>();
        this.participantsList = new ConcurrentLinkedQueue<>();
    }

    public Giveaway update(long guildId,
                           long textChannelId,
                           long userIdLong,
                           long messageId,
                           String title,
                           int countWinners,
                           String time,
                           Long role,
                           boolean isOnlyForSpecificRole,
                           String urlImage,
                           int minParticipants,
                           boolean finishGiveaway,
                           Timestamp endGiveawayDate,
                           Set<String> listUsersHash) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.messageId = messageId;
        this.title = title == null ? "Giveaway" : title;
        this.countWinners = countWinners;
        this.roleId = role;
        this.urlImage = urlImage;
        this.isForSpecificRole = isOnlyForSpecificRole;
        this.minParticipants = minParticipants == 0 ? 2 : minParticipants;
        this.finishGiveaway = finishGiveaway;
        this.endGiveawayDate = endGiveawayDate;
        updateTime(time); //Обновляем время

        if (listUsersHash != null) this.listUsersHash = listUsersHash;
        return this;
    }

    public void startGiveaway(GuildMessageChannel textChannel, boolean predefined) {
        //Отправка сообщения
        if (predefined) {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayLayout(guildId).build())
                    .queue(this::create);
        } else {
            textChannel.sendMessageEmbeds(GiveawayEmbedUtils.giveawayLayout(guildId).build())
                    .queue(message -> {
                        message.addReaction(Emoji.fromUnicode(ReactionEvent.TADA)).queue();
                        create(message);
                    });
        }
    }

    public Timestamp updateTime(final String time) {
        return giveawayTimeHandler.updateTime(this, time);
    }

    private void create(Message message) {
        this.messageId = message.getIdLong();
        giveawaySaving.create(this, message);
    }

    public void addUser(final User user) {
        giveawaySaving.addUser(this, user);
    }

    public void stopGiveaway(final int countWinner) {
        giveawayEnd.stop(this, countWinner);
    }

    public void saveParticipants() {
        giveawaySaving.saveParticipants(guildId, participantsList);
    }

    public boolean isUserContains(String userId) {
        return listUsersHash.contains(userId);
    }

    public void addUserToList(String userId) {
        listUsersHash.add(userId);
    }

    public void addParticipantToList(Participants participants) {
        participantsList.add(participants);
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public Set<String> getListUsers() {
        return new HashSet<>(listUsersHash);
    }

    @Nullable
    public Timestamp getEndGiveawayDate() {
        return this.endGiveawayDate;
    }
}