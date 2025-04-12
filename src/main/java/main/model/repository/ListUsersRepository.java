package main.model.repository;

import main.model.entity.ListUsers;
import main.model.entity.Participants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ListUsersRepository extends JpaRepository<ListUsers, Long> {

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO list_users(giveaway_id, guild_id, created_user_id, nick_name, user_id) " +
            "SELECT ag.message_id, ag.guild_id, ag.created_user_id, p.nick_name, p.user_id " +
            "FROM active_giveaways ag " +
            "JOIN participants p ON ag.message_id = p.message_id " +
            "WHERE ag.message_id = :messageId", nativeQuery = true)
    void saveAllParticipantsToUserList(@Param("messageId") Long messageId);

    List<ListUsers> findAllByGiveawayIdAndCreatedUserId(Long giveawayId, Long createdUserId);

    @Query("SELECT l FROM ListUsers l WHERE l.giveawayId = :messageId AND l.createdUserId = :userId")
    Page<ListUsers> findAllByMessageId(@Param("messageId") Long messageId, @Param("userId") Long userId, Pageable pageable);
}
