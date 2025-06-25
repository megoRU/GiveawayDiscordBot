package main.giveaway;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ParticipantDTO {

    private final long userId;
    private final String nickname;
}
