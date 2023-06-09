package main.model.repository;

import main.model.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    List<Participants> findAllByActiveGiveaways_GuildLongId(Long activeGiveaways_guildLongId);
}