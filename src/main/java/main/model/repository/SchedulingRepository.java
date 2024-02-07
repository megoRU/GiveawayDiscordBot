package main.model.repository;

import main.model.entity.Scheduling;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    @NotNull
    List<Scheduling> findAll();

    Scheduling findByGuildLongId(Long guildLongId);
}