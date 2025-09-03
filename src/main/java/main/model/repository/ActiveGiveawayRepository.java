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

import java.util.List;

@Repository
public interface ActiveGiveawayRepository extends JpaRepository<ActiveGiveaways, Long> {

    @Transactional
    @Modifying
    @Query(value = "UPDATE ActiveGiveaways ac SET ac.finish = true WHERE ac.messageId = :messageId")
    void updateFinishGiveaway(@Param("messageId") Long messageId);

    @Override
    @NotNull
    @EntityGraph(attributePaths = {"participants"})
    List<ActiveGiveaways> findAll();

    List<ActiveGiveaways> findActiveGiveawaysByCreatedUserId(Long createdUserId);

    @Nullable
    List<ActiveGiveaways> findByGuildId(Long guildLongId);

    @Nullable
    ActiveGiveaways findByMessageId(Long messageId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ActiveGiveaways ac WHERE ac.guildId = :guildId")
    void deleteAllByGuildId(@Param("guildId") Long guildId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ActiveGiveaways ac WHERE ac.messageId = :messageId")
    void deleteByMessageId(@Param("messageId") Long messageId);

    @Nullable
    ActiveGiveaways findByCreatedUserIdAndMessageId(Long createdUserId, Long messageId);
}