package main.giveaway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.giveaway.api.response.ParticipantsPOJO;
import main.giveaway.impl.GiftHelper;
import main.giveaway.impl.SetButtons;
import main.giveaway.impl.URLS;
import main.jsonparser.JSONParsers;
import main.messagesevents.SenderMessage;
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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Getter
@Setter
public class Gift {

    private final static Logger LOGGER = Logger.getLogger(Gift.class.getName());
    private final JSONParsers jsonParsers = new JSONParsers();
    private final Set<String> listUsers = new LinkedHashSet<>();
    private final Map<String, String> listUsersHash = new HashMap<>();
    private final Set<String> uniqueWinners = new LinkedHashSet<>();
    private Instant specificTime = null;
    private final Random random = new Random();
    private final long guildId;
    private final long textChannelId;
    private StringBuilder insertQuery = new StringBuilder();
    private AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers = 0;
    private String times;
    private OffsetDateTime offsetTime;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS");
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;
    private volatile Queue<Participants> participantsList = new ArrayDeque<>();
    private volatile Set<ParticipantsPOJO> participantsJSON = new LinkedHashSet<>();

    public Gift(long guildId, long textChannelId, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    private void extracted(EmbedBuilder start, Guild guild, TextChannel channel,
                           String newTitle, String countWinners,
                           String time, Long role, Boolean isOnlyForSpecificRole, String urlImage) {
        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName() + " " + channel.getId()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time
                + "\nRole: " + role
                + "\nisOnlyForSpecificRole: " + isOnlyForSpecificRole
                + "\nurlImage: " + urlImage);
        //Instant для timestamp
        specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        start.setColor(Color.GREEN);
        start.setTitle(newTitle == null ? "Giveaway" : newTitle);

        start.addField(jsonParsers.getLocale("gift_Participants", String.valueOf(guildId)), "`" + count + "`", true);
        start.addField(GiftHelper.setEndingWord(countWinners == null ? "TBA" : countWinners, guildId), countWinners == null ? "TBA" : countWinners, true);

        if (role != null) {

            if (isOnlyForSpecificRole != null && isOnlyForSpecificRole) {
                channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                start.addField("Только для", "<@&" + role + ">", true);
            } else {
                if (role == guildId) {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "@everyone").queue();
                } else {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                }
            }
        }

        start.addField("Attention for Admins: ",
                """
                        May 1, 2022 you will not be able to control the bot
                        without Slash Commands, add them:
                        [Add slash commands](https://discord.com/oauth2/authorize?client_id=808277484524011531&scope=applications.commands%20bot)""",
                false);

        if (time != null) {
            start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));

            if (time.length() > 4) {

                String localTime = time + ":00.001";
                LocalDateTime dateTime = LocalDateTime.parse(localTime, formatter);
                ZoneOffset offset = ZoneOffset.UTC;
                offsetTime = OffsetDateTime.of(dateTime, offset);

                start.setTimestamp(dateTime);

                putTimestamp(new Timestamp(offsetTime.toEpochSecond() * 1000));

            } else {
                times = GiftHelper.getMinutes(time);

                start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)));

                putTimestamp(new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60).getEpochSecond() * 1000));
            }
        }

        if (urlImage != null) {
            start.setImage(urlImage);
        }

    }

    protected void startGift(Guild guild, TextChannel textChannel, String newTitle,
                             String countWinners, String time, Long idUserWhoCreateGiveaway) {
        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time, null, false, null);

        textChannel.sendMessageEmbeds(start.build())
                .setActionRow(SetButtons.getListButtons(String.valueOf(guildId)))
                .queue(message -> updateCollections(guild, countWinners, time, message, null, null, null, newTitle, idUserWhoCreateGiveaway));

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    public void startGift(@NotNull SlashCommandInteractionEvent event, Guild guild,
                          TextChannel textChannel, String newTitle, String countWinners,
                          String time, Long role, Boolean isOnlyForSpecificRole,
                          String urlImage, Long idUserWhoCreateGiveaway) {

        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time, role, isOnlyForSpecificRole, urlImage);

        try {
            event.reply(jsonParsers.getLocale("send_slash_message", guild.getId()).replaceAll("\\{0}", textChannel.getId()))
                    .delay(7, TimeUnit.SECONDS)
                    .flatMap(InteractionHook::deleteOriginal)
                    .queue();
        } catch (Exception e) {
            e.printStackTrace();
        }

        textChannel.sendMessageEmbeds(start.build())
                .setActionRow(SetButtons.getListButtons(String.valueOf(guildId)))
                .queue(message -> updateCollections(guild, countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle, idUserWhoCreateGiveaway));

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Guild guild, String countWinners, String time, Message message, Long role,
                                   Boolean isOnlyForSpecificRole, String urlImage, String title, Long idUserWhoCreateGiveaway) {
        GiveawayRegistry.getInstance().putMessageId(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().putChannelId(guild.getIdLong(), message.getChannel().getId());
        GiveawayRegistry.getInstance().putIdMessagesWithGiveawayButtons(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().putCountWinners(guild.getIdLong(), countWinners);
        GiveawayRegistry.getInstance().putRoleId(guild.getIdLong(), role);
        GiveawayRegistry.getInstance().putIsForSpecificRole(guild.getIdLong(), isOnlyForSpecificRole);
        GiveawayRegistry.getInstance().putUrlImage(guildId, urlImage);
        GiveawayRegistry.getInstance().putTitle(guild.getIdLong(), title == null ? "Giveaway" : title);

        ActiveGiveaways activeGiveaways = new ActiveGiveaways();
        activeGiveaways.setGuildLongId(guildId);
        activeGiveaways.setMessageIdLong(message.getIdLong());
        activeGiveaways.setChannelIdLong(message.getChannel().getIdLong());
        activeGiveaways.setCountWinners(countWinners);
        activeGiveaways.setGiveawayTitle(title);
        activeGiveaways.setRoleIdLong(role);
        activeGiveaways.setIsForSpecificRole(isOnlyForSpecificRole);
        activeGiveaways.setUrlImage(urlImage);
        activeGiveaways.setIdUserWhoCreateGiveaway(idUserWhoCreateGiveaway);

        if (time != null && time.length() > 4) {
            activeGiveaways.setDateEndGiveaway(new Timestamp(offsetTime.toLocalDateTime().atOffset(ZoneOffset.UTC).toEpochSecond() * 1000));
        } else {
            activeGiveaways.setDateEndGiveaway(time == null ? null :
                    new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60)
                            .atOffset(ZoneOffset.UTC)
                            .toEpochSecond() * 1000));
        }
        activeGiveawayRepository.saveAndFlush(activeGiveaways);
    }

    //Добавляет пользователя в StringBuilder
    public void addUserToPoll(final User user) {
        count.incrementAndGet();
        listUsers.add(user.getId());
        listUsersHash.put(user.getId(), user.getId());
        addUserToInsertQuery(user.getName(), user.getAsTag(), user.getIdLong(), guildId);
    }

    //TODO: Может удалять список с кем то. Какой блять список? Уже его нет! А блять понял. Этот participantsList
    //Fixed
    private void executeMultiInsert(long guildIdLong) {
        try {
            if (count.get() > localCountUsers && GiveawayRegistry.getInstance().hasGift(guildIdLong)) {
                localCountUsers = count.get();
                if (!participantsList.isEmpty()) {
                    Set<Participants> temp = new HashSet<>();
                    for (int i = 0; i < participantsList.size(); i++) {
                        temp.add(participantsList.poll());
                    }
                    participantsRepository.saveAllAndFlush(temp);
                    GiftHelper.updateGiveawayMessage(
                            GiveawayRegistry.getInstance().getCountWinners(guildId) == null
                                    ? "TBA"
                                    : GiveawayRegistry.getInstance().getCountWinners(guildId),
                            this.guildId,
                            this.textChannelId,
                            getCount());
                }
            }
        } catch (Exception e) {
            insertQuery = new StringBuilder();
            e.printStackTrace();
            System.out.println("Таблица: " + guildIdLong
                    + " больше не существует, скорее всего Giveaway завершился!\n"
                    + "Очищаем StringBuilder!");
        }
    }

    private void addUserToInsertQuery(final String nickName, final String nickNameTag, final long userIdLong, final long guildIdLong) {
        ActiveGiveaways activeGiveaways = activeGiveawayRepository.getActiveGiveawaysByGuildIdLong(guildIdLong);
        Participants participants = new Participants();
        participants.setUserIdLong(userIdLong);
        participants.setNickName(nickName);
        participants.setNickNameTag(nickNameTag);
        participants.setActiveGiveaways(activeGiveaways);
        participantsList.add(participants);

        participantsJSON.add(new ParticipantsPOJO(
                GiveawayRegistry.getInstance().getIdUserWhoCreateGiveaway(guildId),
                String.valueOf(guildIdLong + Long.parseLong(GiveawayRegistry.getInstance().getMessageId(guildId))),
                guildIdLong,
                userIdLong,
                nickName,
                nickNameTag)
        );
    }

    //Автоматически отправляет в БД данные которые в буфере StringBuilder
    public void autoInsert() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() throws NullPointerException {
                try {
                    if (GiveawayRegistry.getInstance().hasGift(guildId)) {
                        executeMultiInsert(guildId);
                    } else {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }, 2000, 5000);
    }

    /**
     * @throws Exception Throws an exception
     */
    private void sendListUsers() throws Exception {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(participantsJSON);

            LOGGER.info(json);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLS.SAVE_PARTICIPANTS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json")
                    .header("Authorization", BotStartConfig.getBase64())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println(response.statusCode());
                throw new Exception("API not work, or connection refused");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("API not work, or connection refused");
        }
    }

    /**
     * @throws Exception Throws an exception
     */
    private void getWinners(int countWinner) throws Exception {
        try {
            Winners winners = new Winners(countWinner, 0, listUsers.size() - 1);

            LOGGER.info(winners.toString());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URLS.WINNERS))
                    .POST(HttpRequest.BodyPublishers.ofString(winners.toString()))
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String[] winnersArgs = response.body().split(" ");

            List<String> temp = new ArrayList<>(listUsers);

            for (int i = 0; i < winnersArgs.length; i++) {
                uniqueWinners.add("<@" + temp.get(Integer.parseInt(winnersArgs[i])) + ">");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("API not work, or connection refused");
        }
    }

    public void stopGift(final long guildIdLong, final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        if (listUsers.size() < 2) {
            EmbedBuilder notEnoughUsers = new EmbedBuilder();
            notEnoughUsers.setColor(Color.GREEN);
            notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
            notEnoughUsers.setDescription(jsonParsers.getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
            //Отправляет сообщение
            GiftHelper.editMessage(notEnoughUsers, guildIdLong, textChannelId);
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
            GiftHelper.updateGiveawayMessageWithError(
                    GiveawayRegistry.getInstance().getCountWinners(guildId) == null ? "TBA" : GiveawayRegistry.getInstance().getCountWinners(guildId),
                    this.guildId,
                    this.textChannelId,
                    getCount(),
                    countWinner);
            return;
        }

        try {
            sendListUsers();
            //выбираем победителей
            getWinners(countWinner);
        } catch (Exception e) {
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.GREEN);
            errors.setTitle("Errors with API");
            errors.setDescription("Repeat later. Or write to us about it.");

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

            SenderMessage.sendMessage(errors.build(), guildId, textChannelId, buttons);
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setTitle(jsonParsers.getLocale("gift_Giveaway_End", String.valueOf(guildIdLong)));

        if (uniqueWinners.size() == 1) {
            embedBuilder.setDescription(jsonParsers
                    .getLocale("gift_Giveaway_Winner_Mention", String.valueOf(guildIdLong))
                    .replaceAll("\\{0}", String.valueOf(getCount()))
                    + Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "").replaceAll("]", "") +
                    jsonParsers.getLocale("gift_Giveaway_RANDOMORG_one", String.valueOf(guildIdLong)));
        } else {
            embedBuilder.setDescription(jsonParsers
                    .getLocale("gift_Giveaway_Winners", String.valueOf(guildIdLong))
                    .replaceAll("\\{0}", String.valueOf(getCount()))
                    + Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "").replaceAll("]", "") +
                    jsonParsers.getLocale("gift_Giveaway_RANDOMORG_more", String.valueOf(guildIdLong)));
        }

        if (GiveawayRegistry.getInstance().getTitle(guildId) != null
                && !GiveawayRegistry.getInstance().getTitle(guildId).equals("Giveaway")) {
            embedBuilder
                    .addField(jsonParsers.getLocale("gift_what_was_giveaway", String.valueOf(guildIdLong)),
                            GiveawayRegistry.getInstance().getTitle(guildId), false);
        }

        embedBuilder.addField("Giveaway ID", String.valueOf(guildIdLong + Long.parseLong(GiveawayRegistry.getInstance().getMessageId(guildId))), false);

        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));

        //Отправляет сообщение
        GiftHelper.editMessage(embedBuilder, guildId, textChannelId);

        //Удаляет данные из коллекций
        clearingCollections();

        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
    }

    private void putTimestamp(Timestamp timestamp) {
        GiveawayRegistry.getInstance().putEndGiveawayDate(guildId, timestamp);
        BotStartConfig.getQueue().add(new Giveaway(guildId, timestamp));
    }

    private void clearingCollections() {
        try {
            GiveawayRegistry.getInstance().removeGuildFromGiveaway(guildId);
            setCount(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getListUsersHash(String id) {
        return listUsersHash.containsKey(id);
    }

    public int getCount() {
        return count.intValue();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }
}