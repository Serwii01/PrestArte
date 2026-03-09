package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // Buscar el chat de una solicitud concreta
    ChatSession findByLoanRequestId(Long loanRequestId);
}