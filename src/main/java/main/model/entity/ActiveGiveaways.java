package main.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "active_giveaways")
public class ActiveGiveaways {

    @Id
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "count_winners")
    private int countWinners;

    @Column(name = "date_end", nullable = false)
    private Instant endGiveawayDate;

    @Column(name = "title")
    private String title;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "is_for_specific_role")
    private Boolean isForSpecificRole;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "created_user_id", nullable = false)
    private Long createdUserId;

    @Column(name = "url_image")
    private String urlImage;

    @Column(name = "finish", nullable = false)
    private boolean finish;

    @Column(name = "min_participants")
    private Integer minParticipants;

    @OneToMany(mappedBy = "activeGiveaways", cascade = CascadeType.ALL)
    private Set<Participants> participants;
}