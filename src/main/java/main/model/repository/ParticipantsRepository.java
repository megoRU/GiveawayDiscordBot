package main.model.repository;

import main.model.entity.ActiveGiveaways;
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
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    @Query(value = "SELECT * FROM participants p WHERE p.message_id = :messageId", nativeQuery = true)
    List<Participants> findParticipantsByActiveGiveaways(@Param("messageId") Long messageId);

    @Transactional
    @Modifying
    @Query("SELECT ag FROM ActiveGiveaways ag " +
            "JOIN FETCH ag.participants p " +
            "WHERE ag.messageId = :messageId AND ag.createdUserId = :createdUserId")
    List<ActiveGiveaways> findActiveGiveawaysWithParticipants(@Param("messageId") Long messageId, @Param("createdUserId") Long createdUserId);

    @Query("SELECT p FROM Participants p " +
            "JOIN p.activeGiveaways ag " +
            "WHERE ag.messageId = :messageId")
    Page<Participants> findAllByMessageId(@Param("messageId") Long messageId, Pageable pageable);

}