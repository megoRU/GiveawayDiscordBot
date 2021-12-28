package main.model.repository;

import main.model.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    @Query(value = "INSERT INTO participants VALUES :value", nativeQuery = true)
    void insert(@Param("value") String value);

}
