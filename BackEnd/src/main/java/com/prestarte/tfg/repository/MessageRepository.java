package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Acceso a la tabla de mensajes de chat.
 *
 * Permite recuperar el histórico completo de una sesión ordenado por
 * fecha ascendente, que es como se presenta en la interfaz.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Devuelve los mensajes de una sesión ordenados del más antiguo al más reciente. */
    List<Message> findByChatSessionIdOrderBySentAtAsc(Long chatSessionId);
}
