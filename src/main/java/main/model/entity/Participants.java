package main.model.entity;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString
@Table(name = "participants")
public class Participants {

    @Id
    @SequenceGenerator(name = "sequence_id_auto_gen", allocationSize = 100)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_id_auto_gen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "message_id", referencedColumnName = "message_id", nullable = false)
    private ActiveGiveaways activeGiveaways;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nick_name", nullable = false)
    private String nickName;

    public String getUserIdAsString() {
        return String.valueOf(userId);
    }
}