package main.model.repository;

import main.model.entity.ActiveGiveaways;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ActiveGiveawayRepository extends CrudRepository<ActiveGiveaways, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM ActiveGiveaways ac WHERE ac.guildLongId = :guildIdLong")
    void deleteActiveGiveaways(@Param("guildIdLong") Long guildIdLong);


    @Query(value = "SELECT ac FROM ActiveGiveaways ac WHERE ac.guildLongId = :guildIdLong")
    ActiveGiveaways getActiveGiveawaysByGuildIdLong(@Param("guildIdLong") Long guildIdLong);
}