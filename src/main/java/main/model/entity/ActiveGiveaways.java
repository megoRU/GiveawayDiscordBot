package main.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "active_giveaways")
public class ActiveGiveaways {

    @Id
    @Column(name = "guild_long_id", nullable = false)
    private Long guildLongId;

    @Column(name = "message_id_long", nullable = false)
    private Long messageIdLong;

    @Column(name = "channel_id_long", nullable = false)
    private Long channelIdLong;

    @Column(name = "count_winners")
    private String countWinners;

    @Column(name = "date_end_giveaway")
    private Timestamp dateEndGiveaway;

    @Column(name = "giveaway_title")
    private String giveawayTitle;
}