package main.model.entity;


import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participants")
public class Participants {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(cascade = CascadeType.MERGE) // Java хуита. На этом можно закончить
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_long_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    public Participants(ActiveGiveaways activeGiveaways, Long userIdLong, String nickName) {
        this.activeGiveaways = activeGiveaways;
        this.userIdLong = userIdLong;
        this.nickName = nickName;
    }
}