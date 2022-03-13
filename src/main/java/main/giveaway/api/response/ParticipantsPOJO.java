package main.giveaway.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public final class ParticipantsPOJO {

    //UserId
    private final String idUserWhoCreateGiveaway;
    private final String giveawayId;
    private final Long guildIdLong;
    private final Long userIdLong;
    private final String nickName;
    private final String nickNameTag;
}