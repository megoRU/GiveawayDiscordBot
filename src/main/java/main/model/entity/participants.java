package main.model.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "participants")
public class participants {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "guild_long_id", nullable = false)
    private Long guildLongId;

    @Column(name = "user_long_id", nullable = false)
    private Long userIdLong;

    @Column(name = "nick_name", nullable = false)
    private String nickName;
}