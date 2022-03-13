package main.giveaway.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserData {

    private String idUserWhoCreateGiveaway;
    private String giveawayId;

    @Override
    public String toString() {
        return "{" +
                "\"idUserWhoCreateGiveaway\": \"" + idUserWhoCreateGiveaway + "\"" +
                ", \"giveawayId\": \"" + giveawayId + "\"" +
                '}';
    }
}
