package main.model.repository;

import main.model.entity.Scheduling;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    @NotNull
    List<Scheduling> findAll();

    @Nullable
    Scheduling findByGuildLongId(Long guildLongId);

    @Nullable
    Scheduling findByCreatedUserIdAndGuildLongId(Long createdUserId, Long guildLongId);

}