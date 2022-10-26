package main.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participants")
public class Participants extends api.megoru.ru.entity.Participants {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.MERGE) // Java хуита. На этом можно закончить
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_long_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    @Column(name = "nick_name_tag", nullable = false)
    private String nickNameTag;

    //Не удалять
    public Long getGuildIdLong() {
       return activeGiveaways.getGuildLongId();
    }

    //Не удалять
    public String getGiveawayIdLong() {
        return String.valueOf(activeGiveaways.getMessageIdLong());
    }

    //Не удалять
    public String getIdUserWhoCreateGiveaway() {
        return activeGiveaways.getIdUserWhoCreateGiveaway().toString();
    }

    public String getUserIdAsString() {
        return String.valueOf(userIdLong);
    }
}