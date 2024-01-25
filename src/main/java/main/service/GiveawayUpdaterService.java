package main.service;

import lombok.AllArgsConstructor;
import main.giveaway.*;
import main.model.entity.ActiveGiveaways;
import main.model.entity.Participants;
import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GiveawayUpdaterService {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayUpdaterService.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayEnd giveawayEnd;
    private final GiveawaySaving giveawaySaving;
    private final GiveawayMessageHandler giveawayMessageHandler;
    private final ParticipantsRepository participantsRepository;
    private final ListUsersRepository listUsersRepository;
    private final ParticipantsUpdaterService participantsUpdaterService;

    public void updateGiveaway(JDA jda) {
        List<ActiveGiveaways> activeGiveawaysList = activeGiveawayRepository.findAll();

        for (ActiveGiveaways activeGiveaways : activeGiveawaysList) {
            try {
                long guildId = activeGiveaways.getGuildLongId();
                long channelIdLong = activeGiveaways.getChannelIdLong();
                int countWinners = activeGiveaways.getCountWinners();
                long messageIdLong = activeGiveaways.getMessageIdLong();
                String title = activeGiveaways.getGiveawayTitle();
                Timestamp dateEndGiveaway = activeGiveaways.getDateEndGiveaway();
                Long role = activeGiveaways.getRoleIdLong(); // null -> 0
                boolean isForSpecificRole = activeGiveaways.getIsForSpecificRole();
                String urlImage = activeGiveaways.getUrlImage();
                long idUserWhoCreateGiveaway = activeGiveaways.getIdUserWhoCreateGiveaway();
                Integer minParticipants = activeGiveaways.getMinParticipants();
                Long forbiddenRole = activeGiveaways.getForbiddenRole();

                Set<String> participantsMap = activeGiveaways.getParticipants()
                        .stream()
                        .map(Participants::getUserIdLong)
                        .map(String::valueOf)
                        .collect(Collectors.toSet());

                GiveawayBuilder.Builder giveawayBuilder = new GiveawayBuilder.Builder();
                giveawayBuilder.setGiveawayEnd(giveawayEnd);
                giveawayBuilder.setActiveGiveawayRepository(activeGiveawayRepository);
                giveawayBuilder.setGiveawaySaving(giveawaySaving);
                giveawayBuilder.setParticipantsRepository(participantsRepository);
                giveawayBuilder.setListUsersRepository(listUsersRepository);
                giveawayBuilder.setGiveawayMessageHandler(giveawayMessageHandler);

                giveawayBuilder.setTextChannelId(channelIdLong);
                giveawayBuilder.setUserIdLong(idUserWhoCreateGiveaway);
                giveawayBuilder.setMessageId(messageIdLong);
                giveawayBuilder.setGuildId(guildId);
                giveawayBuilder.setTitle(title);
                giveawayBuilder.setCountWinners(countWinners);
                giveawayBuilder.setRoleId(role);
                giveawayBuilder.setEndGiveawayDate(dateEndGiveaway);
                giveawayBuilder.setForSpecificRole(isForSpecificRole);
                giveawayBuilder.setUrlImage(urlImage);
                giveawayBuilder.setMinParticipants(minParticipants);
                giveawayBuilder.setListUsersHash(participantsMap);
                giveawayBuilder.setForbiddenRole(forbiddenRole);

                Giveaway giveaway = giveawayBuilder.build();
                giveaway.setLockEnd(true);
                GiveawayRegistry.getInstance().putGift(guildId, giveaway);

                if (dateEndGiveaway != null) {
                    //TODO: Потом включить
//                    participantsUpdaterService.update(jda);
                    giveaway.setLockEnd(false);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        System.out.println("updateGiveaway()");
    }
}