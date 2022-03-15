package main.giveaway.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StatsPOJO {

    private Integer servers;
    private Integer shards;
    private Integer users;

    @Override
    public String toString() {
        return "{" +
                "\"servers:\"" + servers +
                ", \"shards:\"" + shards  +
                ", \"users:\"" + users +
                "}";
    }
}
