package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a los avisos de los usuarios. */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Últimos avisos de un usuario. */
    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Todos los avisos de un usuario (para la exportación de sus datos). */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId);
}
