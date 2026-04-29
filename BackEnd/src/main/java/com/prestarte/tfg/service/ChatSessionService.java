package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.repository.ChatSessionRepository;
import com.prestarte.tfg.repository.LoanRequestRepository; // Necesitamos esto
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final LoanRequestRepository loanRequestRepository; // Añadido

    @Transactional
    public ChatSessionResponse createChatSession(Long loanRequestId) {
        // 1. Validar que el ID no sea nulo
        if (loanRequestId == null) {
            throw new RuntimeException("El ID del LoanRequest es obligatorio para iniciar un chat");
        }

        // 2. Buscar la solicitud de préstamo real en la DB
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new RuntimeException("No existe una solicitud de préstamo con ID: " + loanRequestId));

        // 3. Opcional: Evitar crear dos chats para la misma solicitud
        // if (chatSessionRepository.existsByLoanRequestId(loanRequestId)) { ... }

        // 4. Construir la nueva sesión
        ChatSession chatSession = ChatSession.builder()
                .loanRequest(loanRequest)
                .estado(ChatSession.EstadoChat.ACTIVO)
                .build();

        ChatSession saved = chatSessionRepository.save(chatSession);
        return mapToResponse(saved);
    }

    public List<ChatSessionResponse> getAllChatSessions() {
        return chatSessionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ChatSessionResponse getChatSessionDtoById(Long id) {
        ChatSession chatSession = chatSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sesión de chat no encontrada"));
        return mapToResponse(chatSession);
    }

    @Transactional
    public ChatSessionResponse closeChatSession(Long chatSessionId) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new RuntimeException("ChatSession no encontrada"));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("El chat ya está cerrado");
        }

        chatSession.setEstado(ChatSession.EstadoChat.CERRADO);
        chatSession.setClosedAt(LocalDateTime.now());

        return mapToResponse(chatSessionRepository.save(chatSession));
    }

    private ChatSessionResponse mapToResponse(ChatSession chatSession) {
        return ChatSessionResponse.builder()
                .id(chatSession.getId())
                .loanRequestId(chatSession.getLoanRequest() != null ? chatSession.getLoanRequest().getId() : null)
                .estado(chatSession.getEstado())
                .createdAt(chatSession.getCreatedAt())
                .closedAt(chatSession.getClosedAt())
                .build();
    }
}