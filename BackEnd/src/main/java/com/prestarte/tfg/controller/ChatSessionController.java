package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * Get-or-create: devuelve la sesión de chat del préstamo, creándola si no
     * existe todavía. El front llama a este endpoint al abrir la pantalla.
     */
    @GetMapping("/loan/{loanId}")
    public ChatSessionResponse getOrCreateForLoan(@PathVariable Long loanId) {
        return chatSessionService.getOrCreateForLoan(loanId);
    }

    @GetMapping
    public List<ChatSessionResponse> getAllChatSessions() {
        return chatSessionService.getAllChatSessions();
    }

    @GetMapping("/{id}")
    public ChatSessionResponse getChatSessionById(@PathVariable Long id) {
        return chatSessionService.getChatSessionDtoById(id);
    }

    @PutMapping("/{id}/close")
    public ChatSessionResponse closeChatSession(@PathVariable Long id) {
        return chatSessionService.closeChatSession(id);
    }
}
