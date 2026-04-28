package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;

    public ChatSession createChatSession(ChatSession chatSession) {
        if (chatSession.getEstado() == null) {
            chatSession.setEstado(ChatSession.EstadoChat.ACTIVO);
        }

        if (chatSession.getLoanRequest() == null || chatSession.getLoanRequest().getId() == null) {
            throw new RuntimeException("LoanRequest es obligatorio");
        }

        return chatSessionRepository.save(chatSession);
    }

    public List<ChatSession> getAllChatSessions() {
        return chatSessionRepository.findAll();
    }

    public Optional<ChatSession> getChatSessionById(Long id) {
        return chatSessionRepository.findById(id);
    }

    public ChatSessionResponse closeChatSession(Long chatSessionId) {
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId)
                .orElseThrow(() -> new RuntimeException("ChatSession no encontrada"));

        if (chatSession.getEstado() == ChatSession.EstadoChat.CERRADO) {
            throw new RuntimeException("El chat ya está cerrado");
        }

        chatSession.setEstado(ChatSession.EstadoChat.CERRADO);
        chatSession.setClosedAt(LocalDateTime.now());

        ChatSession savedChatSession = chatSessionRepository.save(chatSession);

        return mapToResponse(savedChatSession);
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