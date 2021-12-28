package main.giveaway;

import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.jsonparser.JSONParsers;
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
import java.sql.SQLException;
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

    public Gift(long guildId, long textChannelId) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
    }

    private void extracted(EmbedBuilder start, Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName() + " " + channel.getId()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time);

        GiveawayRegistry.getInstance().getTitle().put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
        //Instant для timestamp
        specificTime = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        start.setColor(0x00FF00);
        start.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
        start.addField("Attention for Admins", "[Add slash commands](https://discord.com/oauth2/authorize?client_id=808277484524011531&scope=applications.commands%20bot)", false);

        if (time != null) {

            if (time.length() > 4) {
                String localTime = time + ":00.001";
                LocalDateTime dateTime = LocalDateTime.parse(localTime, formatter);
                ZoneOffset offset = ZoneOffset.UTC;
                offsetTime = OffsetDateTime.of(dateTime, offset);

                start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                        .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                        .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
                start.setTimestamp(OffsetDateTime.parse(String.valueOf(offsetTime)));
                start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
                GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
                        String.valueOf(OffsetDateTime.parse(String.valueOf(offsetTime))));

                BotStartConfig.getQueue().add(new Giveaway(
                        guildId,
                        String.valueOf(OffsetDateTime.parse(String.valueOf(offsetTime)))));

            } else {
                times = getMinutes(time);
                start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                        .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                        .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
                start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)));
                start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
                GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
                        String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times))));

                BotStartConfig.getQueue().add(new Giveaway(
                        guildId,
                        String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)))));
            }
        }
        if (time == null) {
            start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                    .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                    .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
            GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(), "null");
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

    protected void startGift(Guild guild, TextChannel textChannel, String newTitle, String countWinners, String time) {
        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time);

        textChannel.sendMessageEmbeds(start.build()).setActionRow(buttons).queue(message -> {
            updateCollections(guild, countWinners, time, message);
        });


        //TODO: Сделать через репозитории
//        DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());



        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    protected void startGift(@NotNull SlashCommandEvent event, Guild guild, TextChannel textChannel, String newTitle, String countWinners, String time) {
        EmbedBuilder start = new EmbedBuilder();
        extracted(start, guild, textChannel, newTitle, countWinners, time);

        event.reply(jsonParsers.getLocale("send_slash_message", guild.getId()).replaceAll("\\{0}", textChannel.getId()))
                .delay(15, TimeUnit.SECONDS)
                .flatMap(InteractionHook::deleteOriginal)
                .queue();

        textChannel.sendMessageEmbeds(start.build()).setActionRow(buttons).queue(message -> {
            updateCollections(guild, countWinners, time, message);
        });

        //TODO: Сделать через репозитории
//        DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Guild guild, String countWinners, String time, Message message) {
        GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().getChannelId().put(guild.getIdLong(), message.getChannel().getId());
        GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().getCountWinners().put(guild.getIdLong(), countWinners);


        //TODO: Сделать через репозитории

//        if (time != null && time.length() > 4) {
//            DataBase.getInstance().addMessageToDB(guild.getIdLong(),
//                    message.getIdLong(),
//                    message.getChannel().getIdLong(),
//                    countWinners,
//                    String.valueOf(OffsetDateTime.parse(String.valueOf(offsetTime))),
//                    GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
//        } else {
//            DataBase.getInstance().addMessageToDB(guild.getIdLong(),
//                    message.getIdLong(),
//                    message.getChannel().getIdLong(),
//                    countWinners,
//                    time == null ? null : String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times))),
//                    GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
//        }
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
    protected void addUserToPoll(User user) {
        setCount(getCount() + 1);
        listUsers.add(user.getId());
        listUsersHash.put(user.getId(), user.getId());
        addUserToInsertQuery(user.getIdLong());
    }

    private void executeMultiInsert(long guildIdLong) {
        try {
            if (!insertQuery.isEmpty()) {
                //TODO: Сделать через репозитории
                //TODO: Сделать мульти-инсерт для многих значений. Возможно на чистом SQL

//                DataBase.getConnection().createStatement().execute(
//                        "INSERT IGNORE INTO `"
//                                + guildIdLong
//                                + "` (user_long_id) "
//                                + "VALUES" + insertQuery.toString());

                insertQuery = new StringBuilder();
                updateGiveawayMessage(
                        GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null
                                ? "TBA"
                                : GiveawayRegistry.getInstance().getCountWinners().get(guildId),
                        this.guildId,
                        this.textChannelId,
                        getCount());
            }
        } catch (Exception e) {
            insertQuery = new StringBuilder();
            System.out.println("Таблица: " + guildIdLong
                    + " больше не существует, скорее всего Giveaway завершился!\n"
                    + "Очищаем StringBuilder!");
        }
    }

    private void addUserToInsertQuery(long userIdLong) {
        insertQuery.append(insertQuery.length() == 0 ? "" : ",").append("('").append(userIdLong).append("')");
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

            //TODO: Сделать через репозитории
//            DataBase.getInstance().removeMessageFromDB(guildIdLong);
//            DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

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
            editMessage(zero, guildId, textChannelId);
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

            //TODO: Сделать через репозитории
//            DataBase.getInstance().removeMessageFromDB(guildIdLong);
//            DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

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

        //TODO: Сделать через репозитории
//        DataBase.getInstance().removeMessageFromDB(guildIdLong);
//        DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

    }

    private void clearingCollections() {
        try {
            GiveawayRegistry.getInstance().getMessageId().remove(guildId);
            GiveawayRegistry.getInstance().getChannelId().remove(guildId);
            GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().remove(guildId);
            GiveawayRegistry.getInstance().getTitle().remove(guildId);
            GiveawayRegistry.getInstance().removeGift(guildId);
            GiveawayRegistry.getInstance().getEndGiveawayDate().remove(guildId);
            GiveawayRegistry.getInstance().getCountWinners().remove(guildId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getListUsersHash(String id) {
        return listUsersHash.get(id);
    }

}