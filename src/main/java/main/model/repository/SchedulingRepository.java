package main.model.repository;

import main.model.entity.Scheduling;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {

    @NotNull
    List<Scheduling> findAll();

    @Nullable
    Scheduling findByGuildId(Long guildLongId);

    @Nullable
    Scheduling findByIdSalt(String idSalt);

    @Nullable
    Scheduling findByCreatedUserIdAndGuildId(Long createdUserId, Long guildLongId);

    @Nullable
    Scheduling findByCreatedUserIdAndIdSalt(Long createdUserId, String idSalt);

    @Transactional
    @Modifying
    @Query("DELETE FROM Scheduling s WHERE s.idSalt = :idSalt")
    void deleteByIdSalt(String idSalt);
}