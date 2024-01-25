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
    @SequenceGenerator(name = "sequence_id_auto_gen", allocationSize = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_id_auto_gen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_long_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;
}