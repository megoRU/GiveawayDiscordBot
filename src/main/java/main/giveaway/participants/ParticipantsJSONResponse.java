package main.giveaway.participants;

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
public class ParticipantsJSONResponse {

    List<ParticipantsJSON> participantsJSONList;

    public ParticipantsJSONResponse(Set<ParticipantsJSON> participantsJSONList) {
        this.participantsJSONList = new ArrayList<>(participantsJSONList);
    }
}
