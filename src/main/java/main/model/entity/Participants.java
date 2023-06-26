package main.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participants")
public class Participants {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(cascade = CascadeType.MERGE) // Java хуита. На этом можно закончить
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_long_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    public String getUserIdAsString() {
        return String.valueOf(userIdLong);
    }
}