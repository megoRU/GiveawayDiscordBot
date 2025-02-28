package main.model.repository;

import main.model.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {

    @Transactional
    @Modifying
    void deleteByServerId(Long serverId);
}