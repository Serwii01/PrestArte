package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CreateMessageRequest;
import com.prestarte.tfg.model.dto.MessageResponse;
import com.prestarte.tfg.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public MessageResponse createMessage(@Valid @RequestBody CreateMessageRequest request) {
        return messageService.createMessage(request);
    }

    @GetMapping
    public List<MessageResponse> getAllMessages() {
        return messageService.getAllMessages();
    }

    @GetMapping("/{id}")
    public MessageResponse getMessageById(@PathVariable Long id) {
        return messageService.getMessageDtoById(id);
    }

    @GetMapping("/chat/{chatSessionId}")
    public List<MessageResponse> getMessagesByChatSessionId(@PathVariable Long chatSessionId) {
        return messageService.getMessagesByChatSessionId(chatSessionId);
    }
}