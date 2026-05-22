package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.repository.ChatSessionRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona las sesiones de chat asociadas a los préstamos.
 *
 * Garantiza que cada préstamo cuenta con una única sesión, expone la
 * comprobación de participantes que comparten {@link MessageService} y
 * {@link ChatSessionService}, y permite a las partes implicadas cerrar
 * la conversación cuando ya no es necesaria.
 */
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    /**
     * Devuelve la sesión de chat de un préstamo. Si todavía no existe,
     * se crea sobre la marcha en estado ACTIVO. La acción solo está
     * permitida a los participantes del préstamo y al administrador.
     */
    @Transactional
    public ChatSessionResponse getOrCreateForLoan(Long loanRequestId) {
        LoanRequest loan = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", loanRequestId));
        requireParticipant(loan);

        ChatSession session = chatSessionRepository.findByLoanRequestId(loanRequestId)
                .orElseGet(() -> chatSessionRepository.save(ChatSession.builder()
                        .loanRequest(loan)
                        .estado(ChatSession.EstadoChat.ACTIVO)
                        .build()));

        return mapToResponse(session);
    }

    /** Devuelve todas las sesiones de chat. Reservado al administrador. */
    public List<ChatSessionResponse> getAllChatSessions() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException(
                    "Solo los administradores pueden listar todas las sesiones de chat");
        }
        return chatSessionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Devuelve la sesión indicada comprobando los permisos del usuario actual. */
    public ChatSessionResponse getChatSessionDtoById(Long id) {
        ChatSession chatSession = chatSessionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", id));
        requireParticipant(chatSession.getLoanRequest());
        return mapToResponse(chatSession);
    }

    /**
     * Cierra una sesión de chat para que no se puedan enviar nuevos
     * mensajes. El histórico permanece consultable.
     */
    @Transactional
    public ChatSessionResponse closeChatSession(Long chatSessionId) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", chatSessionId));
        requireParticipant(chatSession.getLoanRequest());

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new IllegalStateException("El chat ya está cerrado");
        }
        chatSession.setEstado(ChatSession.EstadoChat.CERRADO);
        chatSession.setClosedAt(LocalDateTime.now());
        return mapToResponse(chatSessionRepository.save(chatSession));
    }

    /**
     * Comprueba que el usuario actual participa en el préstamo
     * indicado. Son participantes el coleccionista dueño, la fundación
     * solicitante, cualquier empresa de transporte que haya intervenido
     * en algún envío del préstamo y los administradores. El método lo
     * reutilizan otros servicios como {@link MessageService}.
     */
    public void requireParticipant(LoanRequest loan) {
        if (currentUser.isAdmin()) return;
        Long collectorId = loan.getArtwork().getCollector().getId();
        Long foundationId = loan.getFoundation().getId();
        if (currentUser.isAnyOf(collectorId, foundationId)) return;

        boolean isTransport = shipmentRepository
                .findByTransportCompanyId(currentUser.currentId())
                .stream()
                .anyMatch(s -> loan.getId().equals(s.getLoanRequest().getId()));
        if (!isTransport) {
            throw new AccessDeniedException("No participas en este préstamo");
        }
    }

    /** Compone el DTO de respuesta a partir de la entidad de sesión. */
    private ChatSessionResponse mapToResponse(ChatSession s) {
        return ChatSessionResponse.builder()
                .id(s.getId())
                .loanRequestId(s.getLoanRequest() != null ? s.getLoanRequest().getId() : null)
                .estado(s.getEstado())
                .createdAt(s.getCreatedAt())
                .closedAt(s.getClosedAt())
                .build();
    }
}
