package main.giveaway;

import api.megoru.ru.entity.Winners;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import main.core.events.ReactionEvent;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import main.threads.StopGiveawayByTimer;
import main.threads.StopGiveawayThread;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@Getter
@Setter
public class Giveaway {

    private static final Logger LOGGER = Logger.getLogger(Giveaway.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    //User LIST
    @Getter(AccessLevel.NONE)
    private ConcurrentHashMap<String, String> listUsersHash;

    private ActiveGiveaways activeGiveaways;

    //GiveawayData
    private long messageId;
    private int countWinners;
    private Long roleId;
    private boolean isForSpecificRole;
    private String urlImage;
    private String title;
    @Nullable
    private Timestamp endGiveawayDate;
    private int minParticipants = 2;
    private long guildId;
    private long textChannelId;
    private long userIdLong;

    private final AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    @Getter(AccessLevel.NONE)
    private final ConcurrentLinkedQueue<Participants> participantsList = new ConcurrentLinkedQueue<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;

    //Service
    private final GiveawayMessageHandler giveawayMessageHandler;
    private final GiveawaySaving giveawaySaving;


    @Autowired
    public Giveaway(ActiveGiveawayRepository activeGiveawayRepository,
                    ParticipantsRepository participantsRepository,
                    ListUsersRepository listUsersRepository,
                    GiveawayMessageHandler giveawayMessageHandler,
                    GiveawaySaving giveawaySaving) {
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
        this.listUsersRepository = listUsersRepository;
        this.giveawayMessageHandler = giveawayMessageHandler;
        this.giveawaySaving = giveawaySaving;
        this.listUsersHash = new ConcurrentHashMap<>();
    }

    public void update(long guildId,
                       long textChannelId,
                       long userIdLong,
                       String title,
                       int countWinners,
                       String time,
                       Long role,
                       boolean isOnlyForSpecificRole,
                       String urlImage,
                       int minParticipants,
                       Map<String, String> listUsersHash) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.title = title == null ? "Giveaway" : title;
        this.countWinners = countWinners;
        this.roleId = role;
        this.urlImage = urlImage;
        this.isForSpecificRole = isOnlyForSpecificRole;
        this.minParticipants = minParticipants == 0 ? 2 : minParticipants;
        updateTime(time); //Обновляем время

        this.listUsersHash = new ConcurrentHashMap<>(listUsersHash);
    }

    public Timestamp updateTime(final String time) {
        GiveawayTimeHandler giveawayTimeHandler = new GiveawayTimeHandler();
        return giveawayTimeHandler.updateTime(this, time);
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

    private void create(Message message) {
        giveawaySaving.create(this, message);
    }

    public void addUser(final User user) {
        giveawaySaving.addUser(this, user);
    }

//    private void multiInsert() {
//        try {
//            if (count.get() > localCountUsers && GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
//                localCountUsers = count.get();
//                if (!participantsList.isEmpty()) {
//                    int size = participantsList.size();
//                    ArrayList<Participants> arrayList = new ArrayList<>(participantsList);
//                    for (int i = 0; i < size; i++) {
//                        Participants poll = participantsList.poll();
//                        arrayList.add(poll);
//                    }
//                    participantsRepository.saveAll(arrayList);
//                }
//                if (participantsList.isEmpty()) {
//                    synchronized (this) {
//                        notifyAll();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            String format = String.format("Таблица: %s больше не существует, скорее всего Giveaway завершился!", guildId);
//            LOGGER.info(format);
//            LOGGER.log(Level.WARNING, e.getMessage(), e);
//        }
//    }

    public void stopGiveaway(final int countWinner) {

    }

    //TODO: Не завершается после завершения
    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    private void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (GiveawayRegistry.getInstance().hasGiveaway(guildId)) {
                        multiInsert();
                    } else {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        }, 2000, 5000);
    }

    public record GiveawayTimerStorage(StopGiveawayByTimer stopGiveawayByTimer, Timer timer) {
    }

    public boolean isUserContains(String userId) {
        return listUsersHash.containsKey(userId);
    }

    public void addUserToList(String userId) {
        listUsersHash.put(userId, userId);
    }

    public void addParticipantToList(Participants participants) {
        participantsList.add(participants);
    }

    public int getParticipantListSize() {
        return participantsList.size();
    }

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }

    @Nullable
    public Timestamp getEndGiveawayDate() {
        return this.endGiveawayDate;
    }

    public boolean isHasFutureTasks() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        Future<?> futureTasks = instance.getFutureTasks(guildId);
        return futureTasks == null;
    }
}