package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Registro de recordatorios enviados. */
@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByKey(String key);
}
