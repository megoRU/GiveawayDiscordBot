package main.model.repository;

import main.model.entity.Scheduling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    @Query(value = "SELECT s FROM Scheduling s")
    List<Scheduling> getAllScheduling();
}