package giveaway;

import db.DataBase;
import jsonparser.JSONParsers;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import threads.Giveaway;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

@Getter
@Setter
public class Gift implements GiftHelper {

    private final static Logger LOGGER = Logger.getLogger(Gift.class.getName());
    private final JSONParsers jsonParsers = new JSONParsers();
    private final List<Button> buttons = new ArrayList<>();
    private final List<String> listUsers = new ArrayList<>();
    private final Map<String, String> listUsersHash = new HashMap<>();
    private final Set<String> uniqueWinners = new HashSet<>();
    private Instant specificTime = null;
    private final Random random = new Random();
    private final long guildId;
    private final long channelId;
    private StringBuilder insertQuery = new StringBuilder();
    private int count;
    private String times;

    public Gift(long guildId, long channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    private void extracted(EmbedBuilder start, Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
        LOGGER.info("\nGuild id: " + guild.getId()
                + "\nTextChannel: " + channel.getName()
                + "\nTitle: " + newTitle
                + "\nCount winners: " + countWinners
                + "\nTime: " + time);

        GiveawayRegistry.getInstance().getTitle().put(guild.getIdLong(), newTitle == null ? "Giveaway" : newTitle);
        Instant timestamp = Instant.now();
        //Instant для timestamp
        specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());

        start.setColor(0x00FF00);
        start.setTitle(GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
        start.addField("Attention for Admins", "[Add slash commands](https://discord.com/oauth2/authorize?client_id=808277484524011531&scope=applications.commands%20bot)", false);

        if (time != null) {
            times = getMinutes(time);
            start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                    .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                    .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
            start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)));
            start.setFooter(jsonParsers.getLocale("gift_Ends_At", guild.getId()));
            GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(),
                    String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times))));

            BotStart.getQueue().add(new Giveaway(
                    guildId,
                    String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)))));
        }
        if (time == null) {
            start.setDescription(jsonParsers.getLocale("gift_Press_Green_Button", guild.getId())
                    .replaceAll("\\{0}", countWinners == null ? "TBA" : countWinners)
                    .replaceAll("\\{1}", setEndingWord(countWinners == null ? "TBA" : countWinners, guildId)) + getCount() + "`");
            GiveawayRegistry.getInstance().getEndGiveawayDate().put(guild.getIdLong(), "null");
        }

        if (BotStart.getMapLanguages().get(guild.getId()) != null) {

            if (BotStart.getMapLanguages().get(guild.getId()).equals("rus")) {
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

    protected void startGift(Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, channel, newTitle, countWinners, time);

        channel.sendMessageEmbeds(start.build()).setActionRow(buttons).queue(message -> {

            GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), message.getId());
            GiveawayRegistry.getInstance().getChannelId().put(guild.getIdLong(), message.getChannel().getId());
            GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild.getIdLong(), message.getId());
            GiveawayRegistry.getInstance().getCountWinners().put(guild.getIdLong(), countWinners);
            DataBase.getInstance().addMessageToDB(guild.getIdLong(),
                    message.getIdLong(),
                    message.getChannel().getIdLong(),
                    countWinners,
                    time == null ? null : String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times))),
                    GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
        });

        DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    protected void startGift(@NotNull SlashCommandEvent event, Guild guild, TextChannel channel, String newTitle, String countWinners, String time) {
        EmbedBuilder start = new EmbedBuilder();
        extracted(start, guild, channel, newTitle, countWinners, time);

        event.replyEmbeds(start.build())
                .addActionRow(buttons)
                .queue(m -> m.retrieveOriginal()
                        .queue(message -> {

                            GiveawayRegistry.getInstance().getMessageId().put(guild.getIdLong(), message.getId());
                            GiveawayRegistry.getInstance().getChannelId().put(guild.getIdLong(), message.getChannel().getId());
                            GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().put(guild.getIdLong(), message.getId());
                            GiveawayRegistry.getInstance().getCountWinners().put(guild.getIdLong(), countWinners);
                            DataBase.getInstance().addMessageToDB(guild.getIdLong(),
                                    message.getIdLong(),
                                    message.getChannel().getIdLong(),
                                    countWinners,
                                    time == null ? null : String.valueOf(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times))),
                                    GiveawayRegistry.getInstance().getTitle().get(guild.getIdLong()));
                        }));

        DataBase.getInstance().createTableWhenGiveawayStart(guild.getId());

        //Вот мы запускаем бесконечный поток.
        autoInsert();
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
                DataBase.getConnection().createStatement().execute(
                        "INSERT IGNORE INTO `"
                                + guildIdLong
                                + "` (user_long_id) "
                                + "VALUES" + insertQuery.toString());
                insertQuery = new StringBuilder();
                updateGiveawayMessage(
                        GiveawayRegistry.getInstance().getCountWinners().get(guildId) == null
                                ? "TBA"
                                : GiveawayRegistry.getInstance().getCountWinners().get(guildId),
                        this.guildId,
                        this.channelId,
                        getCount());
            }
        } catch (SQLException e) {
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

    public void stopGift(long guildIdLong, int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        if (listUsers.size() < 2) {
            EmbedBuilder notEnoughUsers = new EmbedBuilder();
            notEnoughUsers.setColor(0xFF0000);
            notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
            notEnoughUsers.setDescription(jsonParsers.getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
            //Отправляет сообщение
            editMessage(notEnoughUsers, guildId, channelId);
            //Удаляет данные из коллекций
            clearingCollections();

            DataBase.getInstance().removeMessageFromDB(guildIdLong);
            DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

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
            editMessage(zero, guildId, channelId);
            return;
        }
        Instant timestamp = Instant.now();
        Instant specificTime = Instant.ofEpochMilli(timestamp.toEpochMilli());
        if (countWinner > 1) {
            for (int i = 0; i < countWinner; i++) {
                int randomNumber = random.nextInt(listUsers.size());
                uniqueWinners.add("<@" + listUsers.get(randomNumber) + ">");
                listUsers.remove(randomNumber);
            }

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
            editMessage(stopWithMoreWinner, guildId, channelId);

            //Удаляет данные из коллекций
            clearingCollections();

            DataBase.getInstance().removeMessageFromDB(guildIdLong);
            DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

            return;
        }

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
        editMessage(stop, guildId, channelId);

        //Удаляет данные из коллекций
        clearingCollections();

        DataBase.getInstance().removeMessageFromDB(guildIdLong);
        DataBase.getInstance().dropTableWhenGiveawayStop(String.valueOf(guildIdLong));

    }

    private void clearingCollections() {
        GiveawayRegistry.getInstance().getMessageId().remove(guildId);
        GiveawayRegistry.getInstance().getChannelId().remove(guildId);
        GiveawayRegistry.getInstance().getIdMessagesWithGiveawayButtons().remove(guildId);
        GiveawayRegistry.getInstance().getTitle().remove(guildId);
        GiveawayRegistry.getInstance().removeGift(guildId);
        GiveawayRegistry.getInstance().getEndGiveawayDate().remove(guildId);
        GiveawayRegistry.getInstance().getCountWinners().remove(guildId);
    }

    public String getListUsersHash(String id) {
        return listUsersHash.get(id);
    }

}