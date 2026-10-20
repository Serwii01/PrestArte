package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.LoanEventResponse;
import com.prestarte.tfg.model.entity.LoanEvent;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.LoanEventRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registra y consulta el historial de los préstamos.
 *
 * Los servicios de préstamos y envíos llaman a {@link #record} en cada
 * hito. El autor se toma de la sesión actual; si no hay sesión (acciones
 * automáticas del sistema) se guarda sin autor.
 */
@Service
@RequiredArgsConstructor
public class LoanEventService {

    private final LoanEventRepository loanEventRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    /** Registra un cambio de estado del préstamo. */
    @Transactional
    public void recordTransition(LoanRequest loan, LoanRequest.Status from, LoanRequest.Status to,
                                 String message) {
        save(loan, from, to, message, currentActor());
    }

    /** Registra un hito que no cambia el estado del préstamo. */
    @Transactional
    public void record(LoanRequest loan, String message) {
        save(loan, null, null, message, currentActor());
    }

    /** Registra un cambio de estado provocado por el sistema, sin autor. */
    @Transactional
    public void recordSystemTransition(LoanRequest loan, LoanRequest.Status from, LoanRequest.Status to,
                                       String message) {
        save(loan, from, to, message, null);
    }

    /** Historial del préstamo. El llamante debe haber comprobado el acceso. */
    @Transactional(readOnly = true)
    public List<LoanEventResponse> getHistory(Long loanId) {
        return loanEventRepository.findByLoanRequestIdOrderByCreatedAtAscIdAsc(loanId).stream()
                .map(e -> LoanEventResponse.builder()
                        .id(e.getId())
                        .fromStatus(e.getFromStatus() != null ? e.getFromStatus().name() : null)
                        .toStatus(e.getToStatus() != null ? e.getToStatus().name() : null)
                        .message(e.getMessage())
                        .actorName(e.getActor() != null ? e.getActor().getName() : null)
                        .actorRole(e.getActor() != null ? e.getActor().getRole().name() : null)
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    private void save(LoanRequest loan, LoanRequest.Status from, LoanRequest.Status to,
                      String message, User actor) {
        loanEventRepository.save(LoanEvent.builder()
                .loanRequest(loan)
                .fromStatus(from)
                .toStatus(to)
                .message(message.length() > 1000 ? message.substring(0, 1000) : message)
                .actor(actor)
                .build());
    }

    private User currentActor() {
        Long id = currentUser.currentIdOrNull();
        return id != null ? userRepository.getReferenceById(id) : null;
    }
}
