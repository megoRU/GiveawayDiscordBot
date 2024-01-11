package main.giveaway;

import api.megoru.ru.entity.Winners;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.threads.StopGiveawayThread;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service

public class GiveawayEnd {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final Logger LOGGER = Logger.getLogger(GiveawayEnd.class.getName());

    private static final JSONParsers jsonParsers = new JSONParsers();

    private final GiveawayMessageHandler giveawayMessageHandler;
    private final ActiveGiveawayRepository activeGiveawayRepository;

    @Autowired
    public GiveawayEnd(GiveawayMessageHandler giveawayMessageHandler, ActiveGiveawayRepository activeGiveawayRepository) {
        this.giveawayMessageHandler = giveawayMessageHandler;
        this.activeGiveawayRepository = activeGiveawayRepository;
    }

    public void stop(Giveaway giveaway, int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winners: " + countWinner);
        long guildId = giveaway.getGuildId();
        long textChannelId = giveaway.getTextChannelId();
        int minParticipants = giveaway.getMinParticipants();
        long messageId = giveaway.getMessageId();
        int listUsersSize = giveaway.getListUsersSize();
        int participantListSize = giveaway.getParticipantListSize();

        try {
            if (listUsersSize < minParticipants) {

                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(Color.GREEN);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);
                //Отправляет сообщение

                giveawayMessageHandler.editMessage(notEnoughUsers, guildId, textChannelId);

                activeGiveawayRepository.deleteById(guildId);
                //Удаляет данные из коллекций
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.clearingCollections(guildId);
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        final Set<String> uniqueWinners = new LinkedHashSet<>();


        try {
            //выбираем победителей
            if (participantListSize > 0) {
                synchronized (this) {
                    wait(10000L);
                }
            }
            List<Participants> participants = participantsRepository.findAllByActiveGiveaways_GuildLongId(guildId); //TODO: Native use may be
            if (participants.isEmpty()) throw new Exception("participants is Empty");

            LOGGER.info(String.format("Завершаем Giveaway: %s, Участников: %s", guildId, participants.size()));

            Winners winners = new Winners(countWinner, 0, listUsersSize - 1);
            List<String> strings = api.getWinners(winners);
            strings.forEach(s -> uniqueWinners.add("<@" + participants.get(Integer.parseInt(s)).getUserIdLong() + ">"));
        } catch (Exception e) {
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            Future<?> future = instance.getFutureTasks(guildId);

            if (future == null) {
                String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", guildId);
                EmbedBuilder errors = new EmbedBuilder();
                errors.setColor(Color.RED);
                errors.setTitle(errorsWithApi);
                errors.setDescription(errorsDescriptions);
                List<net.dv8tion.jda.api.interactions.components.buttons.Button> buttons = new ArrayList<>();
                buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                updateController.setView(errors.build(), guildId, textChannelId, buttons);

                //Создаем задачу убрать под переменную в бд

                StopGiveawayThread stopGiveawayThread = new StopGiveawayThread(this);
                Future<?> submit = executorService.submit(stopGiveawayThread);
                executorService.shutdown();
                instance.putFutureTasks(guildId, submit);
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
            return;
        }

        EmbedBuilder urlEmbedded = new EmbedBuilder();
        urlEmbedded.setColor(Color.GREEN);
        String url = GiveawayUtils.getDiscordUrlMessage(guildId, textChannelId, messageId);
        String winnerArray = Arrays.toString(uniqueWinners.toArray())
                .replaceAll("\\[", "")
                .replaceAll("]", "");

        String winnersContent;
        if (uniqueWinners.size() == 1) {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations", guildId), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            giveawayMessageHandler.editMessage(embedBuilder, guildId, textChannelId);
        } else {
            winnersContent = String.format(jsonParsers.getLocale("gift_congratulations_many", guildId), winnerArray);
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);
            urlEmbedded.setDescription(giftUrl);
            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId);
            giveawayMessageHandler.editMessage(embedBuilder, guildId, textChannelId);
        }

        giveawayMessageHandler.sendMessage(urlEmbedded.build(), winnersContent, guildId, textChannelId);

        listUsersRepository.saveAllParticipantsToUserList(guildId);
        activeGiveawayRepository.deleteById(guildId);

        //Удаляет данные из коллекций
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.clearingCollections(guildId);

        Future<?> future = instance.getFutureTasks(guildId);
        if (future != null) {
            future.cancel(true);
            instance.removeFutureTasks(guildId);
        }
    }
}