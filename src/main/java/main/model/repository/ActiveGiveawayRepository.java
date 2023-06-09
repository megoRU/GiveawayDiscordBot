package main.model.repository;

import main.model.entity.ActiveGiveaways;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Repository
public interface ActiveGiveawayRepository extends JpaRepository<ActiveGiveaways, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE ActiveGiveaways ac SET ac.dateEndGiveaway = :dateEndGiveaway WHERE ac.guildLongId = :guildIdLong")
    void updateGiveawayTime(@Param("guildIdLong") Long guildIdLong, @Param("dateEndGiveaway") Timestamp dateEndGiveaway);
}