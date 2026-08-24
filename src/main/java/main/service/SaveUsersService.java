package main.service;

import lombok.AllArgsConstructor;
import main.giveaway.*;
import main.model.entity.ActiveGiveaways;
import main.model.repository.ActiveGiveawayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@AllArgsConstructor
public class SaveUsersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveUsersService.class.getName());

    private final ActiveGiveawayRepository activeGiveawayRepository;
    private final GiveawayRepositoryService giveawayRepositoryService;

    public void saveParticipants() {
        // No-op or fetch active giveaways if needed. Since participants are added when user reacts or through user handler directly saving to DB,
        // we keep safety handling for any active giveaways if needed.
    }
}