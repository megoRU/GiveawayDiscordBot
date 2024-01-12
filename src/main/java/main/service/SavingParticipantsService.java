package main.service;

import main.giveaway.Giveaway;
import main.giveaway.GiveawayRegistry;
import org.springframework.stereotype.Service;

@Service
public class SavingParticipantsService {

    public void save() {
        GiveawayRegistry instance = GiveawayRegistry.getInstance();
        instance.getGiveaways().forEach(Giveaway::saveParticipants);
    }
}