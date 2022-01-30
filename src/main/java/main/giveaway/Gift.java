package main.giveaway;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ParticipantsRepository;
import main.threads.Giveaway;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
@Setter
public class Gift implements GiftHelper {

    private final static Logger LOGGER = Logger.getLogger(Gift.class.getName());
    private final String URL = "http://45.140.167.181:8085/api/winners";
    private final JSONParsers jsonParsers = new JSONParsers();
    private final List<Button> buttons = new ArrayList<>();
    private final List<String> listUsers = new ArrayList<>();
    private final Map<String, String> listUsersHash = new HashMap<>();
    private final Set<String> uniqueWinners = new HashSet<>();
    private Instant specificTime = null;
    private final Random random = new Random();
    private final long guildId;
    private final long textChannelId;
    private StringBuilder insertQuery = new StringBuilder();
    private int count;
    private String times;
    private OffsetDateTime offsetTime;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS");
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private Set<Participants> participantsList = new HashSet<>();

    public Gift(long guildId, long textChannelId, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    private void extracted(EmbedBuilder start, Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName() + " " + channel.getId()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time);

        GiveawayRegistry.getInstance().putTitle(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
        //Instant для timestamp
        specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        start.setColor(0x00FF00);
        start.setTitle(GiveawayRegistry.getInstance().getTitle(guild.getIdLong()));
        start.addField("Attention for Admins: \nMay 1, 2022 you will not be able to control the bot \nwithout SlashCommands add them: ", "[Add slash commands](https://discord.com/oauth2/authorize?client_id=808277484524011531&scope=applications.commands%20bot)", false);

        if (time != null) {

            if (time.length() > 4) {

                String localTime = time + ":00.001";
                LocalDateTime dateTime = LocalDateTime.parse(localTime, formatter);
                ZoneOffset offset = ZoneOffset.UTC;
                offsetTime = OffsetDateTime.of(dateTime, offset);

                start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                        .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                        .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");

                start.setTimestamp(dateTime);
                start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));

                GiveawayRegistry.getInstance().putEndGiveawayDate(guild.getIdLong(),
                        new Timestamp(offsetTime.toEpochSecond() * 1000));

                BotStartConfig.getQueue().add(new Giveaway(guildId,
                        new Timestamp(offsetTime.toEpochSecond() * 1000)));
            } else {
                times = getMinutes(time);
                start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                        .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                        .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");

                start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)));
                start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));

                GiveawayRegistry.getInstance().putEndGiveawayDate(guild.getIdLong(),
                        new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60).getEpochSecond() * 1000));

                BotStartConfig.getQueue().add(new Giveaway(
                        guildId,
                        new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60).getEpochSecond() * 1000)));
            }
        }
        if (time == null) {
            start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                    .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                    .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
            GiveawayRegistry.getInstance().putEndGiveawayDate(guild.getIdLong(), null);
        }

        if (BotStartConfig.getMapLanguages().get(guild.getId()) != null) {

            if (BotStartConfig.getMapLanguages().get(guild.getId()).equals("rus")) {
                buttons.add(Button.success(guildId + ":" + ReactionsButton.PRESENT,
                        jsonParsers.getLocale("gift_Press_Me_Button", guild.getId()) + "⠀ "));
            } else {
                buttons.add(Button.success(guildId + ":" + ReactionsButton.PRESENT,
                        jsonParsers.getLocale("gift_Press_Me_Button", guild.getId()) + "⠀⠀⠀⠀⠀⠀⠀⠀"));
            }
        } else {
            buttons.add(Button.success(guildId + ":" + ReactionsButton.PRESENT,
                    jsonParsers.getLocale("gift_Press_Me_Button", guild.getId()) + "⠀⠀⠀⠀⠀⠀⠀⠀"));
        }
    }

    protected void startGift(@NotNull SlashCommandEvent event, Guild guild, TextChannel textChannel, String newTitle, String countWinners, String time) {
        EmbedBuilder start = new EmbedBuilder();
        extracted(start, guild, textChannel, newTitle, countWinners, time);

        event.reply(jsonParsers.getLocale("send_slash_message", guild.getId()).replaceAll("\\{0}", textChannel.getId()))
                .delay(15, TimeUnit.SECONDS)
                .flatMap(InteractionHook::deleteOriginal)
                .queue();

        textChannel.sendMessageEmbeds(start.build()).setActionRow(buttons).queue(message -> updateCollections(guild, countWinners, time, message));

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Guild guild, String countWinners, String time, Message message) {
        GiveawayRegistry.getInstance().putMessageId(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().putChannelId(guild.getIdLong(), message.getChannel().getId());
        GiveawayRegistry.getInstance().putIdMessagesWithGiveawayButtons(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().putCountWinners(guild.getIdLong(), countWinners);

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(GiveawayRegistry.getInstance().getTitle(guild.getIdLong()));

        if (time != null && time.length() > 4) {
            activeGiveaways.setDateEndGiveaway(new Timestamp(offsetTime.toLocalDateTime().atOffset(ZoneOffset.UTC).toEpochSecond() * 1000));
        } else {
            activeGiveaways.setDateEndGiveaway(time == null ? null :
                    new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60)
                            .atOffset(ZoneOffset.UTC)
                            .toEpochSecond() * 1000));
        }
        activeGiveawayRepository.save(activeGiveaways);
    }

    private String getMinutes(String time) {
        String symbol = time.substring(time.length() - 1);
        time = time.substring(0, time.length() - 1);

        if (symbol.equals("m") || symbol.equals("м")) {
            return time;
        }

        if (symbol.equals("h") || symbol.equals("ч")) {
            return String.valueOf(Integer.parseInt(time) * 60);
        }

        if (symbol.equals("d") || symbol.equals("д")) {
            return String.valueOf(Integer.parseInt(time) * 1440);
        }
        return "5";
    }

    //Добавляет пользователя в StringBuilder
    protected void addUserToPoll(final User user) {
        setCount(getCount() + 1);
        listUsers.add(user.getId());
        listUsersHash.put(user.getId(), user.getId());
        addUserToInsertQuery(user.getName(), user.getIdLong(), guildId);
    }

    //TODO: Может удалять список с кем то. Какой блять список? Уже его нет! А блять понял. Этот participantsList
    private void executeMultiInsert(long guildIdLong) {
        try {
            if (!participantsList.isEmpty()) {
                int countParticipants = participantsList.size();
                Set<Participants> tempParticipantsList = participantsList;

                participantsRepository.saveAll(participantsList);
                synchronized (this) {
                    if (countParticipants == participantsList.size()) {
                        participantsList.clear();
                    } else {
                        participantsList.removeAll(tempParticipantsList);
                    }
                }
                updateGiveawayMessage(
                        GiveawayRegistry.getInstance().getCountWinners(guildId) == null
                                ? "TBA"
                                : GiveawayRegistry.getInstance().getCountWinners(guildId),
                        this.guildId,
                        this.textChannelId,
                        getCount());
            }
        } catch (Exception e) {
            insertQuery = new StringBuilder();
            e.printStackTrace();
            System.out.println("Таблица: " + guildIdLong
                    + " больше не существует, скорее всего Giveaway завершился!\n"
                    + "Очищаем StringBuilder!");
        }
    }

    private void addUserToInsertQuery(final String nickName, final long userIdLong, final long guildIdLong) {
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.getActiveGiveawaysByGuildIdLong(guildIdLong);
        Participants participants = new Participants();
        participants.setUserIdLong(userIdLong);
        participants.setNickName(nickName);
        participants.setActiveGiveaways(activeGiveaways);
        participantsList.add(participants);
    }

    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    public void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    executeMultiInsert(guildId);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }, 1, 5000);
    }

    private void getWinners(int countWinner) {
        try {
            Winners winners = new Winners(countWinner, 0, listUsers.size() - 1);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .POST(HttpRequest.BodyPublishers.ofString(winners.toString()))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String[] winnersArgs = response.body().split(" ");

            for (int i = 0; i < winnersArgs.length; i++) {
                uniqueWinners.add("<@" + listUsers.get(Integer.parseInt(winnersArgs[i])) + ">");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopGift(long guildIdLong, int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        if (listUsers.size() < 2) {
            EmbedBuilder notEnoughUsers = new EmbedBuilder();
            notEnoughUsers.setColor(0xFF0000);
            notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
            notEnoughUsers.setDescription(jsonParsers.getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
            //Отправляет сообщение
            editMessage(notEnoughUsers, guildIdLong, textChannelId);
            //Удаляет данные из коллекций
            clearingCollections();

            activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
            return;
        }

        if (countWinner == 0 || countWinner >= listUsers.size()) {
            EmbedBuilder zero = new EmbedBuilder();
            zero.setColor(0xFF8000);
            zero.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));

            zero.setDescription(jsonParsers
                    .getLocale("gift_Invalid_Number_Description", String.valueOf(guildIdLong))
                    .replaceAll("\\{0}", String.valueOf(countWinner))
                    .replaceAll("\\{1}", String.valueOf(getCount())));
            //Отправляет сообщение
            updateGiveawayMessageWithError(
                    GiveawayRegistry.getInstance().getCountWinners(guildId) == null ? "TBA" : GiveawayRegistry.getInstance().getCountWinners(guildId),
                    this.guildId,
                    this.textChannelId,
                    getCount(),
                    countWinner);
            return;
        }
        Instant timestamp = Instant.now();
        Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
        if (countWinner > 1) {
            //выбираем победителей
            getWinners(countWinner);

            EmbedBuilder stopWithMoreWinner = new EmbedBuilder();
            stopWithMoreWinner.setColor(0x00FF00);
            stopWithMoreWinner.setTitle(jsonParsers.getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));
            stopWithMoreWinner.setDescription(jsonParsers
                    .getLocale("gift_Giveaway_Winners", String.valueOf(guildIdLong))
                    .replaceAll("\\{0}", String.valueOf(getCount()))
                    + Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "").replaceAll("]", ""));
            stopWithMoreWinner.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
            stopWithMoreWinner.setFooter(jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));


            //Отправляет сообщение
            editMessage(stopWithMoreWinner, guildId, textChannelId);

            //Удаляет данные из коллекций
            clearingCollections();

            activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
            return;
        }

        //выбираем победителей
        getWinners(countWinner);

        EmbedBuilder stop = new EmbedBuilder();
        stop.setColor(0x00FF00);
        stop.setTitle(jsonParsers
                .getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));

        stop.setDescription(jsonParsers
                .getLocale("gift_Giveaway_Winner_Mention", String.valueOf(guildIdLong))
                .replaceAll("\\{0}", String.valueOf(getCount()))
                + listUsers.get(random.nextInt(listUsers.size())) + ">");
        stop.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)));
        stop.setFooter(jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));

        //Отправляет сообщение
        editMessage(stop, guildId, textChannelId);

        //Удаляет данные из коллекций
        clearingCollections();

        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
    }

    private void clearingCollections() {
        try {
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getListUsersHash(String id) {
        return listUsersHash.get(id);
    }

}