package main.giveaway;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.WinnersAndParticipants;
import api.megoru.ru.impl.MegoruAPIImpl;
import lombok.Getter;
import lombok.Setter;
import main.config.BotStartConfig;
import main.giveaway.impl.ChecksClass;
import main.giveaway.impl.GiftHelper;
import main.giveaway.reactions.Reactions;
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

import static main.giveaway.impl.URLS.getDiscordUrlMessage;

@Getter
@Setter
public class Gift {

    private static final Logger LOGGER = Logger.getLogger(Gift.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS");
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPIImpl(System.getenv("BASE64_PASSWORD"));

    //User LIST
    private final Map<String, String> listUsersHash = new LinkedHashMap<>();
    private final Set<String> uniqueWinners = new LinkedHashSet<>();

    //Time
    private Instant specificTime;
    private String times;
    private OffsetDateTime offsetTime;

    //USER DATA
    private final long guildId;
    private final long textChannelId;
    private final long userIdLong;

    private StringBuilder insertQuery = new StringBuilder();
    private AtomicInteger count = new AtomicInteger(0);
    private int localCountUsers;

    //DTO
    private volatile Queue<Participants> participantsList = new ArrayDeque<>();
    private volatile Set<api.megoru.ru.entity.Participants> participantsJSON = new LinkedHashSet<>();

    //REPO
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ParticipantsRepository participantsRepository;

    public Gift(long guildId, long textChannelId, long userIdLong, ActiveGiveawayRepository activeGiveawayRepository, ParticipantsRepository participantsRepository) {
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.userIdLong = userIdLong;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.participantsRepository = participantsRepository;
    }

    private void extracted(EmbedBuilder start, Guild guild, TextChannel channel,
                           String newTitle, String countWinners,
                           String time, Long role, boolean isOnlyForSpecificRole, String urlImage) {
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

        String title = newTitle == null ? "Giveaway" : newTitle;

        start.setColor(Color.GREEN);
        start.setTitle(title);
        start.appendDescription(jsonParsers.getLocale("git_react", guild.getId()));

        if (role != null) {
            if (isOnlyForSpecificRole) {
                channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                start.appendDescription(jsonParsers.getLocale("gift_OnlyFor", guild.getId()) + "<@&" + role + ">");
            } else {
                if (role == guildId) {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "@everyone").queue();
                } else {
                    channel.sendMessage(jsonParsers.getLocale("gift_notification_for_this_role", guild.getId()) + "<@&" + role + ">").queue();
                }
            }
        }

        String footer;
        if (countWinners == null) {
            footer = "1 " + GiftHelper.setEndingWord(1, guildId);
        } else {
            footer = countWinners + " " + GiftHelper.setEndingWord(Integer.parseInt(countWinners), guildId);
        }

        start.setFooter(footer);

        if (time != null) {
            start.setFooter(footer + " | " + jsonParsers.getLocale("gift_Ends_At", guild.getId()));

            if (time.length() > 4) {

                String localTime = time + ":00.001";
                LocalDateTime dateTime = LocalDateTime.parse(localTime, formatter);
                ZoneOffset offset = ZoneOffset.UTC;
                offsetTime = OffsetDateTime.of(dateTime, offset);

                start.setTimestamp(dateTime);

                start.appendDescription("\nEnds: <t:" + offsetTime.toEpochSecond() + ":R> (<t:" + offsetTime.toEpochSecond() + ":f>)");

                putTimestamp(new Timestamp(offsetTime.toEpochSecond() * 1000));

            } else {
                times = GiftHelper.getMinutes(time);

                long toEpochSecond = OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)).toEpochSecond();
                start.setTimestamp(OffsetDateTime.parse(String.valueOf(specificTime)).plusMinutes(Long.parseLong(times)));

                start.appendDescription("\nEnds: <t:" + toEpochSecond + ":R> (<t:" + toEpochSecond + ":f>)");

                putTimestamp(new Timestamp(specificTime.plusSeconds(Long.parseLong(times) * 60).getEpochSecond() * 1000));
            }
        }

        start.appendDescription("\nHosted by: " + "<@" + userIdLong + ">");

        if (urlImage != null) {
            start.setImage(urlImage);
        }

    }

    protected void startGift(Guild guild, TextChannel textChannel, String newTitle,
                             String countWinners, String time, Long idUserWhoCreateGiveaway) {
        EmbedBuilder start = new EmbedBuilder();

        extracted(start, guild, textChannel, newTitle, countWinners, time, null, false, null);

        textChannel.sendMessageEmbeds(start.build())
                .queue(message -> {
                            message.addReaction(Reactions.TADA).queue();
                            updateCollections(guild, countWinners, time, message, null, false, null, newTitle, idUserWhoCreateGiveaway);
                        }
                );

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    public void startGift(@NotNull SlashCommandInteractionEvent event, Guild guild,
                          TextChannel textChannel, String newTitle, String countWinners,
                          String time, Long role, boolean isOnlyForSpecificRole,
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
                .queue(message -> {
                    message.addReaction(Reactions.TADA).queue();
                    updateCollections(guild, countWinners, time, message, role, isOnlyForSpecificRole, urlImage, newTitle, idUserWhoCreateGiveaway);
                });

        //Вот мы запускаем бесконечный поток.
        autoInsert();
    }

    private void updateCollections(Guild guild, String countWinners, String time, Message message, Long role,
                                   Boolean isOnlyForSpecificRole, String urlImage, String title, Long idUserWhoCreateGiveaway) {
        GiveawayRegistry.getInstance().putMessageId(guild.getIdLong(), message.getId());
        GiveawayRegistry.getInstance().putChannelId(guild.getIdLong(), message.getChannel().getId());
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
        if (!listUsersHash.containsKey(user.getId())) {
            count.incrementAndGet();
            listUsersHash.put(user.getId(), user.getId());
            addUserToInsertQuery(user.getName(), user.getAsTag(), user.getIdLong(), guildId);
        }
    }

    private EmbedBuilder embedBuilder(Color color, String winners, final int countWinner) {
        EmbedBuilder embedBuilder = null;
        try {
            embedBuilder = new EmbedBuilder();
            LOGGER.info("\nwinners: " + winners);
            embedBuilder.setColor(color);
            embedBuilder.setTitle(GiveawayRegistry.getInstance().getTitle(guildId));


            if (countWinner == 1) {
                embedBuilder.appendDescription(jsonParsers.getLocale("gift_winner", String.valueOf(guildId)) + winners);
            } else {
                embedBuilder.appendDescription(jsonParsers.getLocale("gift_winners", String.valueOf(guildId)) + winners);
            }

            String footer = GiftHelper.setEndingWord(countWinner, guildId);
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter(countWinner + " " + footer + " | " + jsonParsers.getLocale("gift_Ends", String.valueOf(guildId)));


            if (GiveawayRegistry.getInstance().getIsForSpecificRole(guildId)) {
                embedBuilder.appendDescription(jsonParsers.getLocale("gift_OnlyFor", String.valueOf(guildId))
                        + " <@&" + GiveawayRegistry.getInstance().getRoleId(guildId) + ">");
            }

            embedBuilder.appendDescription("\nHosted by: " + "<@" + userIdLong + ">");
            embedBuilder.appendDescription("\nGiveaway ID: `" + (guildId + Long.parseLong(GiveawayRegistry.getInstance().getMessageId(guildId))) + "`");

            if (GiveawayRegistry.getInstance().getUrlImage(guildId) != null) {
                embedBuilder.setImage(GiveawayRegistry.getInstance().getUrlImage(guildId));
            }


        } catch (
                Exception e) {
            e.printStackTrace();
        }

        return embedBuilder;
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

        participantsJSON.add(new api.megoru.ru.entity.Participants(
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
    private void getWinners(int countWinner) throws Exception {
        if (participantsJSON.isEmpty()) throw new Exception("participantsJSON is Empty");

        try {
            Winners winners = new Winners(countWinner, 0, listUsersHash.size() - 1);

            WinnersAndParticipants winnersAndParticipants = new WinnersAndParticipants();
            winnersAndParticipants.setUpdate(true);
            winnersAndParticipants.setWinners(winners);
            winnersAndParticipants.setUserList(participantsJSON);

            LOGGER.info(winners.toString());

            String[] strings = api.setWinners(winnersAndParticipants);

            List<String> temp = new LinkedList<>(listUsersHash.values());

            if (strings == null) throw new Exception("API not work, or connection refused");

            for (int i = 0; i < strings.length; i++) {
                uniqueWinners.add("<@" + temp.get(Integer.parseInt(strings[i])) + ">");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("API not work, or connection refused");
        }
    }

    public void stopGift(final long guildIdLong, final int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winner: " + countWinner);
        ChecksClass checksClass = new ChecksClass(activeGiveawayRepository);
        GiftHelper giftHelper = new GiftHelper(activeGiveawayRepository);
        if (checksClass.isGuildDeleted(guildId)) return;
        try {
            if (listUsersHash.size() < 2) {
                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(jsonParsers.getLocale("gift_Not_Enough_Users", String.valueOf(guildIdLong)));
                notEnoughUsers.setDescription(jsonParsers.getLocale("gift_Giveaway_Deleted", String.valueOf(guildIdLong)));
                //Отправляет сообщение
                giftHelper.editMessage(notEnoughUsers, guildIdLong, textChannelId);

                activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
                //Удаляет данные из коллекций
                clearingCollections();

                return;
            }

            if (countWinner == 0 || countWinner >= listUsersHash.size()) {
                EmbedBuilder zero = new EmbedBuilder();
                zero.setColor(0xFF8000);
                zero.setTitle(jsonParsers.getLocale("gift_Invalid_Number", String.valueOf(guildIdLong)));

                zero.setDescription(jsonParsers
                        .getLocale("gift_Invalid_Number_Description", String.valueOf(guildIdLong))
                        .replaceAll("\\{0}", String.valueOf(countWinner))
                        .replaceAll("\\{1}", String.valueOf(getCount())));

                giftHelper.getMessageDescription(guildId, textChannelId).whenComplete((m, throwable) -> {

                    //Отправляет сообщение
                    giftHelper.editMessage(zero, guildIdLong, textChannelId);

                    try {
                        Thread.sleep(15000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    zero.setTitle(GiveawayRegistry.getInstance().getTitle(guildId));
                    zero.setColor(Color.GREEN);
                    zero.setTitle(GiveawayRegistry.getInstance().getTitle(guildId));
                    zero.setDescription(m.getEmbeds().get(0).getDescription());

                    giftHelper.editMessage(zero, guildIdLong, textChannelId);
                    if (throwable != null) {
                        System.out.println(throwable.getMessage());
                        if (throwable.getMessage().contains("10008: Unknown Message")) {
                            activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);
                            clearingCollections();
                            System.out.println("gift stop: Удаляем Giveaway");
                        }
                    }
                });
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //выбираем победителей
            getWinners(countWinner);
        } catch (Exception e) {
            EmbedBuilder errors = new EmbedBuilder();
            errors.setColor(Color.RED);
            errors.setTitle("Errors with API");
            errors.setDescription("Repeat later. Or write to us about it.");
            errors.appendDescription("\nYou have not completed the Giveaway");

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));

            SenderMessage.sendMessage(errors.build(), guildId, textChannelId, buttons);
            return;
        }

        EmbedBuilder winners = new EmbedBuilder();
        winners.setColor(Color.GREEN);

        String messageId = GiveawayRegistry.getInstance().getMessageId(guildId);
        String url = getDiscordUrlMessage(String.valueOf(this.guildId), String.valueOf(this.textChannelId), messageId);

        if (uniqueWinners.size() == 1) {
            winners.setDescription(jsonParsers.getLocale("gift_congratulations",
                    String.valueOf(guildIdLong)).replaceAll("\\{0}", url)
                    + Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "")
                    .replaceAll("]", ""));

            giftHelper.editMessage(
                    embedBuilder(Color.GREEN, Arrays.toString(uniqueWinners.toArray())
                            .replaceAll("\\[", "")
                            .replaceAll("]", ""), countWinner),
                    guildId,
                    textChannelId);
        } else {
            winners.setDescription(jsonParsers.getLocale("gift_congratulations_many",
                    String.valueOf(guildIdLong)).replaceAll("\\{0}", url)
                    + Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "")
                    .replaceAll("]", ""));

            giftHelper.editMessage(
                    embedBuilder(Color.GREEN, Arrays.toString(uniqueWinners.toArray())
                            .replaceAll("\\[", "")
                            .replaceAll("]", ""), countWinner),
                    guildId,
                    textChannelId);
        }

        SenderMessage.sendMessage(winners.build(), guildId, textChannelId);

        activeGiveawayRepository.deleteActiveGiveaways(guildIdLong);

        //Удаляет данные из коллекций
        clearingCollections();
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

    public int getListUsersSize() {
        return listUsersHash.size();
    }

    public int getCount() {
        return count.intValue();
    }

    public void setCount(int count) {
        this.count.set(count);
        this.localCountUsers = count;
    }
}