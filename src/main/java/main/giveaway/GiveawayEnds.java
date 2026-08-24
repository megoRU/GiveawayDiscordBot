package main.giveaway;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.impl.MegoruAPI;
import lombok.AllArgsConstructor;
import main.config.BotStart;
import main.controller.UpdateController;
import main.jsonparser.JSONParsers;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.service.GiveawayRepositoryService;
import main.service.ParticipantsGrabber;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class GiveawayEnds {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayEnds.class.getName());
    private static final JSONParsers jsonParsers = new JSONParsers();

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();

    private final GiveawayRepositoryService giveawayRepositoryService;

    public void cancel(ActiveGiveaways activeGiveaways, UpdateController updateController) {
        long guildId = activeGiveaways.getGuildId();
        long textChannelId = activeGiveaways.getChannelId();
        long messageId = activeGiveaways.getMessageId();

        Color userColor = GiveawayUtils.getUserColor(guildId);

        String giveawayWasCanceled = jsonParsers.getLocale("giveaway_was_canceled", guildId);
        String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

        EmbedBuilder cancel = new EmbedBuilder();
        cancel.setColor(userColor);
        cancel.setTitle(giveawayWasCanceled);
        cancel.setDescription(giftGiveawayDeleted);

        updateController
                .setViewRest(cancel.build(), guildId, textChannelId, messageId)
                .queue(_ -> giveawayRepositoryService.deleteGiveaway(messageId)
                        , throwable -> LOGGER.warn("Не удалось отправить сообщение", throwable));
    }

    public void stop(ActiveGiveaways activeGiveaways, int countWinner, UpdateController updateController) {
        long guildId = activeGiveaways.getGuildId();
        long textChannelId = activeGiveaways.getChannelId();
        boolean finishGiveaway = activeGiveaways.isFinish();
        long messageId = activeGiveaways.getMessageId();
        int minParticipants = activeGiveaways.getMinParticipants();
        boolean predefined = activeGiveaways.isPredefined();

        try {
            GiveawayUserHandler giveawayUserHandler = new GiveawayUserHandler(giveawayRepositoryService);
            Set<ParticipantDTO> participantSet;

            if (predefined) {
                List<Participants> participants = giveawayRepositoryService.findAllParticipants(messageId);

                participantSet = participants
                        .stream()
                        .map(p -> new ParticipantDTO(p.getUserId(), p.getNickName()))
                        .collect(Collectors.toSet());
            } else {
                ParticipantsGrabber participantsGrabber = new ParticipantsGrabber(giveawayRepositoryService);
                participantSet = participantsGrabber.get(activeGiveaways);
            }

            List<Long> participants = participantSet
                    .stream()
                    .map(ParticipantDTO::getUserId)
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
                        giveawayRepositoryService.setFinishGiveaway(messageId);

                        String errorsWithApi = jsonParsers.getLocale("errors_with_api", guildId);
                        String errorsDescriptions = jsonParsers.getLocale("errors_descriptions", guildId);
                        EmbedBuilder errors = new EmbedBuilder();
                        errors.setColor(Color.RED);
                        errors.setTitle(errorsWithApi);
                        errors.setDescription(errorsDescriptions);
                        List<Button> buttons = new ArrayList<>();
                        buttons.add(Button.link("https://discord.gg/MhEzJNDf", "Support"));

                        updateController.setView(errors.build(), guildId, textChannelId, buttons);
                    }
                    LOGGER.error(e.getMessage(), e);
                    return;
                }

                String url = GiveawayUtils.getDiscordUrlMessage(guildId, textChannelId, activeGiveaways.getMessageId());
                String giftUrl = String.format(jsonParsers.getLocale("gift_url", guildId), url);

                String winnerArray = Arrays.toString(uniqueWinners.toArray())
                        .replaceAll("\\[", "")
                        .replace("]", "");

                JDA jda = BotStart.getJda();

                EmbedBuilder embedBuilder = GiveawayEmbedUtils.giveawayEnd(winnerArray, countWinner, activeGiveaways);

                if (embedBuilder == null) {
                    giveawayRepositoryService.deleteGiveaway(messageId);
                    return;
                }

                updateController.setViewRest(embedBuilder.build(), guildId, textChannelId, messageId).queue(
                        _ -> {
                            if (guildText != null) {
                                String string = guildText.replaceAll("@winner", winnerArray)
                                        .replaceAll("@link", giftUrl);

                                updateController.setViewRest(jda, string, guildId, textChannelId)
                                        .queue(
                                                _ -> {
                                                    //Сохраняем
                                                    giveawayUserHandler.saveUser(activeGiveaways, participantSet.stream().toList());

                                                    giveawayRepositoryService.backupAllParticipants(messageId);
                                                    giveawayRepositoryService.deleteGiveaway(messageId);
                                                },
                                                throwable -> LOGGER.warn("Не удалось отправить сообщение", throwable)
                                        );
                            } else {
                                String winnersContent = uniqueWinners.size() == 1
                                        ? String.format(jsonParsers.getLocale("gift_congratulations", guildId), winnerArray, giftUrl)
                                        : String.format(jsonParsers.getLocale("gift_congratulations_many", guildId), winnerArray, giftUrl);

                                updateController.setViewRest(jda, winnersContent, guildId, textChannelId)
                                        .queue(
                                                _ -> {
                                                    //Сохраняем
                                                    giveawayUserHandler.saveUser(activeGiveaways, participantSet.stream().toList());

                                                    giveawayRepositoryService.backupAllParticipants(messageId);
                                                    giveawayRepositoryService.deleteGiveaway(messageId);
                                                },
                                                throwable -> LOGGER.warn("Не удалось отправить сообщение", throwable)
                                        );
                            }
                        },
                        throwable -> {
                            giveawayRepositoryService.setFinishGiveaway(messageId);
                            LOGGER.warn("Не удалось обновить embed Giveaway", throwable);
                        }
                );
            } else if (participants.isEmpty()) {
                cancel(activeGiveaways, updateController);
            } else {
                try {
                    String giftNotEnoughUsers = jsonParsers.getLocale("gift_not_enough_users", guildId);
                    String giftGiveawayDeleted = jsonParsers.getLocale("gift_giveaway_deleted", guildId);

                    EmbedBuilder notEnoughUsers = new EmbedBuilder();
                    notEnoughUsers.setColor(userColor);
                    notEnoughUsers.setTitle(giftNotEnoughUsers);
                    notEnoughUsers.setDescription(giftGiveawayDeleted);

                    //Отправляет сообщение
                    updateController.setViewRest(notEnoughUsers.build(), guildId, textChannelId, messageId).queue(
                            _ -> giveawayRepositoryService.deleteGiveaway(messageId),
                            throwable -> LOGGER.warn("Не удалось отправить сообщение", throwable)
                    );
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}