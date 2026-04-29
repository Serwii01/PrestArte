package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.repository.ChatSessionRepository;
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

    @Transactional
    public ChatSessionResponse createChatSession(ChatSession chatSession) {
        if (chatSession.getLoanRequest() == null || chatSession.getLoanRequest().getId() == null) {
            throw new RuntimeException("LoanRequest es obligatorio para iniciar un chat");
        }

        if (chatSession.getEstado() == null) {
            chatSession.setEstado(ChatSession.EstadoChat.ACTIVO);
        }

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
                .loanRequestId(chatSession.getLoanRequest().getId())
                .estado(chatSession.getEstado())
                .createdAt(chatSession.getCreatedAt())
                .closedAt(chatSession.getClosedAt())
                .build();
    }
}