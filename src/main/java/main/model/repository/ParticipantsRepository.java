package main.model.repository;

import main.model.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    @Query(value = "SELECT p FROM Participants p WHERE p.activeGiveaways.guildLongId = :guildIdLong")
    List<Participants> getParticipantsByGuildIdLong(@Param("guildIdLong") Long guildIdLong);
}
