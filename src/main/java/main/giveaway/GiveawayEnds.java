package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.jsonparser.JSONParsers;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import main.service.GiveawayUpdateListUser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

@AllArgsConstructor
public class GiveawayEnds {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayEnds.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    private final GiveawayRepositoryService giveawayRepositoryService;

    public void cancel(Giveaway giveaway, UpdateController updateController) {
        long guildId = giveaway.getGuildId();
        long textChannelId = giveaway.getTextChannelId();
        GiveawayData giveawayData = giveaway.getGiveawayData();
        long messageId = giveawayData.getMessageId();

        Color userColor = GiveawayUtils.getUserColor(guildId);

        String giveawayWasCanceled = jsonParsers.getLocale("giveaway_was_canceled", guildId);
        String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);


        EmbedBuilder cancel = new EmbedBuilder();
        cancel.setColor(userColor);
        cancel.setTitle(giveawayWasCanceled);
        cancel.setDescription(giftGiveawayDeleted);

        updateController.setView(cancel.build(), guildId, textChannelId, messageId);
    }

    public void stop(Giveaway giveaway, int countWinner, UpdateController updateController) {
        long guildId = giveaway.getGuildId();
        long textChannelId = giveaway.getTextChannelId();
        boolean finishGiveaway = giveaway.isFinishGiveaway();
        GiveawayData giveawayData = giveaway.getGiveawayData();
        long messageId = giveawayData.getMessageId();
        int minParticipants = giveawayData.getMinParticipants();

        GiveawayUpdateListUser giveawayUpdateListUser = new GiveawayUpdateListUser(giveawayRepositoryService);
        giveawayUpdateListUser.updateGiveawayByGuild(giveaway);

        Collection<Long> values = giveawayData.getParticipantsList().values().stream()
                .map(Long::valueOf)
                .toList();

        List<Long> participants = Stream.concat(values.stream(), giveawayRepositoryService.findAllParticipants(messageId)
                        .stream()
                        .map(Participants::getUserId))
                .distinct()
                .toList();

        final Set<String> uniqueWinners = new LinkedHashSet<>();

        Color userColor = GiveawayUtils.getUserColor(guildId);
        String guildText = GiveawayUtils.getGuildText(guildId);
        int participantsSize = participants.size();

        if (countWinner <= participantsSize && countWinner >= minParticipants) {
            try {
                LOGGER.info("Завершаем Giveaway: {}, Участников: {}", guildId, participantsSize);

                if (participantsSize > 1) {
                    Winners winners = new Winners(countWinner, 0, participantsSize - 1);
                    List<String> strings = api.getWinners(winners);
                    for (String string : strings) {
                        uniqueWinners.add("<@" + participants.get(Integer.parseInt(string)) + ">");
                    }
                } else {
                    uniqueWinners.add("<@" + participants.getFirst() + ">");
                }
            } catch (Exception e) {
                if (!finishGiveaway) {
                    giveaway.setFinishGiveaway(true);
                    giveawayRepositoryService.setFinishGiveaway(messageId);

                    String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                    String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", guildId);
                    EmbedBuilder errors = new EmbedBuilder();
                    errors.setColor(Color.RED);
                    errors.setTitle(errorsWithApi);
                    errors.setDescription(errorsDescriptions);
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(Button.link("https://discord.gg/UrWG3R683d", "Support"));
                    updateController.setView(errors.build(), guildId, textChannelId, buttons);
                }
                LOGGER.error(e.getMessage(), e);
                return;
            }

            String url = GiveawayUtils.getDiscordUrlMessage(guildId, textChannelId, giveawayData.getMessageId());
            String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);

            String winnerArray = Arrays.toString(uniqueWinners.toArray())
                    .replaceAll("\\[", "")
                    .replaceAll("]", "");

            JDA jda = BotStart.getJda();

            String winnersContent;

            EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, guildId, messageId);
            GiveawayRegistry instance = GiveawayRegistry.getInstance();

            if (embedBuilder == null) {
                instance.removeGiveaway(messageId);
                return;
            }

            updateController.setView(embedBuilder.build(), guildId, textChannelId, messageId);

            if (guildText != null) {
                String string = guildText.replaceAll("@winner", winnerArray).replaceAll("@link", giftUrl);
                updateController.setView(jda, string, guildId, textChannelId);
            } else {
                if (uniqueWinners.size() == 1) {
                    winnersContent = String.format(jsonParsers.getLocale("gift_congratulations", guildId), winnerArray, giftUrl);
                } else {
                    winnersContent = String.format(jsonParsers.getLocale("gift_congratulations_many", guildId), winnerArray, giftUrl);
                }
                updateController.setView(jda, winnersContent, guildId, textChannelId);
            }

            giveaway.setRemoved(true);
            //Удаляет данные из коллекций
            instance.removeGiveaway(messageId);

            giveawayRepositoryService.backupAllParticipants(messageId);
            giveawayRepositoryService.deleteGiveaway(messageId);
        } else {
            try {
                String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
                String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

                EmbedBuilder notEnoughUsers = new EmbedBuilder();
                notEnoughUsers.setColor(userColor);
                notEnoughUsers.setTitle(giftNotEnoughUsers);
                notEnoughUsers.setDescription(giftGiveawayDeleted);

                //Отправляет сообщение
                updateController.setView(notEnoughUsers.build(), guildId, textChannelId, messageId);

                giveawayRepositoryService.deleteGiveaway(messageId);
                GiveawayRegistry instance = GiveawayRegistry.getInstance();
                instance.removeGiveaway(messageId);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}