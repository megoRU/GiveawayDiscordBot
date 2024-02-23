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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    public String getUserIdAsString() {
        return String.valueOf(userId);
    }
}