package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
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

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    /**
     * Devuelve la sesión de chat del préstamo, creándola si no existe.
     * Solo los participantes del préstamo (coleccionista, museo, transportista
     * asignado) o un admin pueden invocarlo.
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

    public List<ChatSessionResponse> getAllChatSessions() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException(
                    "Solo los administradores pueden listar todas las sesiones de chat");
        }
        return chatSessionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ChatSessionResponse getChatSessionDtoById(Long id) {
        ChatSession chatSession = chatSessionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Sesión de chat", id));
        requireParticipant(chatSession.getLoanRequest());
        return mapToResponse(chatSession);
    }

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
     * Comprueba que el usuario actual es uno de los participantes del préstamo
     * (collector dueño, foundation solicitante, transport del shipment, o admin).
     * Reutilizado por MessageService.
     */
    public void requireParticipant(LoanRequest loan) {
        if (currentUser.isAdmin()) return;
        Long collectorId = loan.getArtwork().getCollector().getId();
        Long foundationId = loan.getFoundation().getId();
        if (currentUser.isAnyOf(collectorId, foundationId)) return;

        // Cualquier empresa de transporte que haya participado en este préstamo
        // (incluso una con presupuesto rechazado tras reasignación) puede entrar
        // al chat para responder dudas o consultar el histórico.
        boolean isTransport = shipmentRepository
                .findByTransportCompanyId(currentUser.currentId())
                .stream()
                .anyMatch(s -> loan.getId().equals(s.getLoanRequest().getId()));
        if (!isTransport) {
            throw new AccessDeniedException("No participas en este préstamo");
        }
    }

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
