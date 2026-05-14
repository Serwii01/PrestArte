package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    /** Chat asociado a una solicitud concreta. */
    Optional<ChatSession> findByLoanRequestId(Long loanRequestId);
}