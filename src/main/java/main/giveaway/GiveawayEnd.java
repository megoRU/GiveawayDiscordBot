package main.giveaway;

import api.megoru.ru.entity.Winners;
import main.giveaway.api.GiveawayAPI;
import main.giveaway.utils.GiveawayUtils;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service

public class GiveawayEnd {

    private static final Logger LOGGER = Logger.getLogger(GiveawayEnd.class.getName());

    private static final JSONParsers jsonParsers = new JSONParsers();

    private final GiveawayMessageHandler giveawayMessageHandler;
    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final ListUsersRepository listUsersRepository;

    private final GiveawayAPI giveawayAPI;

    @Autowired
    public GiveawayEnd(GiveawayMessageHandler giveawayMessageHandler,
                       ActiveGiveawayRepository activeGiveawayRepository,
                       ListUsersRepository listUsersRepository) {
        this.giveawayMessageHandler = giveawayMessageHandler;
        this.activeGiveawayRepository = activeGiveawayRepository;
        this.listUsersRepository = listUsersRepository;
        this.giveawayAPI = new GiveawayAPI();
    }

    public void stop(Giveaway giveaway, int countWinner) {
        LOGGER.info("\nstopGift method" + "\nCount winners: " + countWinner);
        long guildId = giveaway.getGuildId();
        long textChannelId = giveaway.getTextChannelId();
        int minParticipants = giveaway.getMinParticipants();
        long messageId = giveaway.getMessageId();
        int listUsersSize = giveaway.getListUsersSize();

        if (listUsersSize < minParticipants) {

            String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
            String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

            EmbedBuilder notEnoughUsers = new EmbedBuilder();
            notEnoughUsers.setColor(Color.GREEN);
            notEnoughUsers.setTitle(giftNotEnoughUsers);
            notEnoughUsers.setDescription(giftGiveawayDeleted);
            giveaway.clearParticipant();
            //Отправляет сообщение
            giveawayMessageHandler.editMessage(notEnoughUsers, guildId, textChannelId);

            activeGiveawayRepository.deleteById(guildId);
            //Удаляет данные из коллекций
            GiveawayRegistry instance = GiveawayRegistry.getInstance();
            instance.removeGiveaway(guildId);
            return;
        }

        Set<String> uniqueWinners = new LinkedHashSet<>();
        Set<String> listUsers = giveaway.getListUsers();

        LOGGER.info(String.format("""
                \n
                Guild_ID %s
                USERS_listUsers: %s
                \n
                """, guildId, Arrays.toString(listUsers.toArray())));

        try {
            Optional<ActiveGiveaways> optionalActiveGiveaways = activeGiveawayRepository.findById(guildId);
            if (optionalActiveGiveaways.isPresent()) {
                Set<String> participants = optionalActiveGiveaways
                        .get()
                        .getParticipants()
                        .stream()
                        .map(Participants::getUserIdLong)
                        .map(String::valueOf)
                        .collect(Collectors.toSet());

                LOGGER.info(String.format("""
                        \n
                        Guild_ID %s
                        USERS_participants: %s
                        \n
                        """, guildId, Arrays.toString(participants.toArray())));

                if (listUsers.size() != participants.size()) {
                    listUsers.addAll(participants);
                }

                LOGGER.info(String.format("""
                        \n
                        Guild_ID %s
                        USERS_listUsers: %s
                        \n
                        """, guildId, Arrays.toString(listUsers.toArray())));

                List<String> stringList = new ArrayList<>(listUsers);
                if (stringList.isEmpty()) throw new Exception("participants is Empty");

                LOGGER.info(String.format("Завершаем Giveaway: %s, Участников: %s", guildId, participants.size()));

                Winners winners = new Winners(countWinner, 0, stringList.size() - 1);
                List<String> strings = giveawayAPI.getWinners(winners);
                strings.forEach(s -> uniqueWinners.add("<@" + stringList.get(Integer.parseInt(s)) + ">"));
            } else {
                return;
            }
        } catch (Exception e) {
            Optional<ActiveGiveaways> optionalActiveGiveaways = activeGiveawayRepository.findById(guildId);
            if (optionalActiveGiveaways.isPresent()) {
                ActiveGiveaways activeGiveaways = optionalActiveGiveaways.get();
                if (!activeGiveaways.isFinishGiveaway()) {
                    String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                    String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", guildId);
                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(Color.RED);
                    errors.setTitle(errorsWithApi);
                    errors.setDescription(errorsDescriptions);
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                    giveawayMessageHandler.sendMessage(errors.build(), guildId, textChannelId, buttons);

                    activeGiveaways.setFinishGiveaway(true);
                    activeGiveawayRepository.save(activeGiveaways);
                }
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
            return;
        }

        EmbedBuilder urlEmbedded = new EmbedBuilder();
        Color userColor = GiveawayUtils.getUserColor(guildId);
        urlEmbedded.setColor(userColor);
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

        giveaway.clearParticipant();
        //Удаляет данные из коллекций
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.removeGiveaway(guildId);

        giveawayMessageHandler.sendMessage(urlEmbedded.build(), winnersContent, guildId, textChannelId);

        listUsersRepository.saveAllParticipantsToUserList(guildId);
        activeGiveawayRepository.deleteById(guildId);
    }
}