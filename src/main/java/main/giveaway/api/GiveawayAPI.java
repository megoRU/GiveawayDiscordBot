package main.giveaway.api;

import api.megoru.ru.impl.MegoruAPI;
import org.springframework.stereotype.Service;

@Service
public class GiveawayAPI {

    //API
    private final MegoruAPI api = new MegoruAPI.Builder().build();



}
