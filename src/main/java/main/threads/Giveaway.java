package main.threads;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class Giveaway {

    private final Long ID_GUILD;
    private final Timestamp TIME;
}
