package main.giveaway.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantsResponse {

    List<ParticipantsPOJO> participantsJSONList;

    public ParticipantsResponse(Set<ParticipantsPOJO> participantsJSONList) {
        this.participantsJSONList = new ArrayList<>(participantsJSONList);
    }
}
