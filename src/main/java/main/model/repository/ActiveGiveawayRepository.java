package main.model.repository;

import main.model.entity.ActiveGiveaways;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveGiveawayRepository extends JpaRepository<ActiveGiveaways, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE ActiveGiveaways ac SET ac.dateEndGiveaway = :dateEndGiveaway WHERE ac.guildLongId = :guildIdLong")
    void updateGiveawayTime(@Param("guildIdLong") Long guildIdLong, @Param("dateEndGiveaway") Timestamp dateEndGiveaway);

    @Override
    @NotNull
    @EntityGraph(attributePaths = {"participants"})
    List<ActiveGiveaways> findAll();

    @Nullable
    ActiveGiveaways findByGuildLongId(Long guildLongId);

    @Nullable
    ActiveGiveaways findByIdUserWhoCreateGiveawayAndGuildLongId(Long idUserWhoCreateGiveaway, Long guildLongId);
}