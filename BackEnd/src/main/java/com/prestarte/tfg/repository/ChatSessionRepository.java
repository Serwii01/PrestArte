package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a la tabla de sesiones de chat.
 *
 * Las sesiones de chat se asocian uno a uno con los préstamos, por lo
 * que la consulta principal busca la sesión a partir del préstamo.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /** Devuelve la sesión de chat asociada al préstamo indicado, si existe. */
    Optional<ChatSession> findByLoanRequestId(Long loanRequestId);
}
