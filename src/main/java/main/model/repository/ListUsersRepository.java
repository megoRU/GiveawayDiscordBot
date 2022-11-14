package main.model.repository;

import main.model.entity.ListUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ListUsersRepository extends JpaRepository<ListUsers, Long> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO list_users(giveaway_id, guild_id, created_user_id, nick_name, nick_name_tag, user_id) " +
            "SELECT ag.message_id_long, ag.guild_long_id, ag.id_user_who_create_giveaway, p.nick_name, p.nick_name_tag, p.user_long_id " +
            "FROM active_giveaways ag, participants p " +
            "WHERE ag.guild_long_id = :guildId AND p.guild_id = :guildId", nativeQuery = true)
    void saveAllParticipantsToUserList(@Param("guildId") Long guildId);

    @Query("SELECT lu FROM ListUsers lu WHERE lu.giveawayId = :giveawayId AND lu.createdUserId = :createdUserId")
    List<ListUsers> findAllByGiveawayId(@Param("giveawayId") Long giveawayId, @Param("createdUserId") Long createdUserId);
}
