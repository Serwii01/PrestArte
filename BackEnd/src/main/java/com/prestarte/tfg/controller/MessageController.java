package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CreateMessageRequest;
import com.prestarte.tfg.model.dto.MessageResponse;
import com.prestarte.tfg.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Endpoints REST relacionados con los mensajes del chat.
 *
 * Cubren la creación de mensajes (con o sin archivo adjunto) y la
 * lectura del histórico de una sesión. El emisor se toma siempre del
 * token JWT, no del cuerpo de la petición.
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** Crea un mensaje de texto en una sesión de chat. */
    @PostMapping
    public MessageResponse createMessage(@Valid @RequestBody CreateMessageRequest request) {
        return messageService.createMessage(request);
    }

    /**
     * Crea un mensaje con archivo adjunto (imagen o documento). El
     * campo de texto es opcional y el tipo del mensaje se deduce del
     * MIME del archivo.
     */
    @PostMapping(value = "/with-file", consumes = "multipart/form-data")
    public MessageResponse createMessageWithFile(
            @RequestParam Long chatSessionId,
            @RequestParam(required = false) String content,
            @RequestPart("file") MultipartFile file) throws IOException {
        return messageService.createMessageWithFile(chatSessionId, content, file);
    }

    /** Devuelve todos los mensajes del sistema (reservado a auditoría). */
    @GetMapping
    public List<MessageResponse> getAllMessages() {
        return messageService.getAllMessages();
    }

    /** Devuelve un mensaje concreto a partir de su identificador. */
    @GetMapping("/{id}")
    public MessageResponse getMessageById(@PathVariable Long id) {
        return messageService.getMessageDtoById(id);
    }

    /** Devuelve los mensajes de una sesión ordenados cronológicamente. */
    @GetMapping("/chat/{chatSessionId}")
    public List<MessageResponse> getMessagesByChatSessionId(@PathVariable Long chatSessionId) {
        return messageService.getMessagesByChatSessionId(chatSessionId);
    }
}
