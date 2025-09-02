package main.model.repository;

import main.model.entity.UserZoneId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserZoneIdRepository extends JpaRepository<UserZoneId, Long> {

    @NotNull List<UserZoneId> findAll();

    @Nullable
    UserZoneId findByUserId(Long userId);
}
