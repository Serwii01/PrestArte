package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.model.entity.ChatSession;
import com.prestarte.tfg.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    public ChatSessionResponse createChatSession(@RequestBody ChatSession chatSession) {
        return chatSessionService.createChatSession(chatSession);
    }

    @PutMapping("/{id}/close")
    public ChatSessionResponse closeChatSession(@PathVariable Long id) {
        return chatSessionService.closeChatSession(id);
    }

    @GetMapping
    public List<ChatSessionResponse> getAllChatSessions() {
        return chatSessionService.getAllChatSessions();
    }

    @GetMapping("/{id}")
    public ChatSessionResponse getChatSessionById(@PathVariable Long id) {
        return chatSessionService.getChatSessionDtoById(id);
    }
}