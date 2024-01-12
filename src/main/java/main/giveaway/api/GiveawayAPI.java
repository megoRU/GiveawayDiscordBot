package main.giveaway.api;

import api.megoru.ru.entity.Winners;
import api.megoru.ru.entity.exceptions.UnsuccessfulHttpException;
import api.megoru.ru.impl.MegoruAPI;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class GiveawayAPI {

    //API
    private final MegoruAPI api;


    public GiveawayAPI() {
        this.api = new MegoruAPI.Builder().build();;
    }

    public List<String> getWinners(Winners winners) throws UnsuccessfulHttpException, IOException {
       return api.getWinners(winners);
    }

}