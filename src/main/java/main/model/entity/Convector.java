package main.model.entity;

import api.megoru.ru.entity.Participants;
import main.giveaway.GiveawayRegistry;

import java.util.LinkedList;
import java.util.List;

public class Convector {

    private final List<main.model.entity.Participants> participantsListAPI;

    public Convector(List<main.model.entity.Participants> participantsListAPI1) {
        this.participantsListAPI = new LinkedList<>(participantsListAPI1);
    }

    public List<Participants> getList() {
        List<Participants> temp = new LinkedList<>();
        for (main.model.entity.Participants value : participantsListAPI) {
            Long giveawayGuildId = value.getGiveawayGuildId();
            String messageId = GiveawayRegistry.getInstance().getMessageId(giveawayGuildId);

            Participants participants = new Participants(
                    value.getIdUserWhoCreateGiveaway(),
                    String.valueOf(giveawayGuildId + Long.parseLong(messageId)),
                    value.getGiveawayGuildId(),
                    value.getUserIdLong(),
                    value.getNickName(),
                    value.getNickNameTag()
            );
            temp.add(participants);
        }
        return temp;
    }
}