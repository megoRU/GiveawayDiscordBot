package main.model.repository;

import main.model.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    @Query(value = "SELECT * FROM participants p WHERE p.message_id = :messageId", nativeQuery = true)
    List<Participants> findParticipantsByActiveGiveaways(@Param("messageId") Long messageId);
}