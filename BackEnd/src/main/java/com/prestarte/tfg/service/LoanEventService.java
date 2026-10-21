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
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /** Registra un cambio de estado del préstamo y avisa a las demás partes (también por email). */
    @Transactional
    public void recordTransition(LoanRequest loan, LoanRequest.Status from, LoanRequest.Status to,
                                 String message) {
        save(loan, from, to, message, currentActor());
        notificationService.notifyLoanParties(loan, message, true);
    }

    /** Registra un hito que no cambia el estado del préstamo; avisa solo dentro de la aplicación. */
    @Transactional
    public void record(LoanRequest loan, String message) {
        save(loan, null, null, message, currentActor());
        notificationService.notifyLoanParties(loan, message, false);
    }

    /**
     * Registra un hito que requiere que alguien actúe (un seguro que
     * aprobar, cambios pedidos, una incidencia...): avisa también por email.
     */
    @Transactional
    public void recordImportant(LoanRequest loan, String message) {
        save(loan, null, null, message, currentActor());
        notificationService.notifyLoanParties(loan, message, true);
    }

    /**
     * Registra un cambio de estado provocado por el sistema, sin autor. El
     * aviso por email lo envía quien llama, con un texto específico.
     */
    @Transactional
    public void recordSystemTransition(LoanRequest loan, LoanRequest.Status from, LoanRequest.Status to,
                                       String message) {
        save(loan, from, to, message, null);
        notificationService.notifyLoanParties(loan, message, false);
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
