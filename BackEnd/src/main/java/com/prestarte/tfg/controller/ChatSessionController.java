package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    public ChatSessionResponse createChatSession(@RequestBody Map<String, Long> payload) {
        // Extraemos el ID del JSON {"loanRequestId": 1}
        return chatSessionService.createChatSession(payload.get("loanRequestId"));
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