package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // Obtener el historial de un chat ordenado por fecha
    List<Message> findByChatSessionIdOrderByTimestampAsc(Long sessionId);
}