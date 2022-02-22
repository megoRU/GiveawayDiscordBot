package main.model.repository;

import main.model.entity.Prefixs;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Deprecated
public interface PrefixRepository extends CrudRepository<Prefixs, String> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM Prefixs p WHERE p.serverId = :guildIdLong")
    void deletePrefix(@Param("guildIdLong") String guildIdLong);

    @Query(value = "SELECT p FROM Prefixs p")
    List<Prefixs> getPrefixs();
}