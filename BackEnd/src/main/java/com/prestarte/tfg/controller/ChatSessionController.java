package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ChatSessionResponse;
import com.prestarte.tfg.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST relacionados con las sesiones de chat.
 *
 * Permiten abrir o recuperar la sesión asociada a un préstamo,
 * consultarla y cerrarla. La sesión se crea automáticamente la
 * primera vez que un participante accede al chat del préstamo.
 */
@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * Devuelve la sesión de chat de un préstamo. Si todavía no existe,
     * se crea sobre la marcha. Es el endpoint que el frontend
     * consulta al abrir la pantalla de mensajes.
     */
    @GetMapping("/loan/{loanId}")
    public ChatSessionResponse getOrCreateForLoan(@PathVariable Long loanId) {
        return chatSessionService.getOrCreateForLoan(loanId);
    }

    /** Devuelve todas las sesiones de chat existentes (reservado al administrador). */
    @GetMapping
    public List<ChatSessionResponse> getAllChatSessions() {
        return chatSessionService.getAllChatSessions();
    }

    /** Devuelve los datos de una sesión concreta. */
    @GetMapping("/{id}")
    public ChatSessionResponse getChatSessionById(@PathVariable Long id) {
        return chatSessionService.getChatSessionDtoById(id);
    }

    /** Cierra la sesión indicada y evita que se puedan enviar más mensajes. */
    @PutMapping("/{id}/close")
    public ChatSessionResponse closeChatSession(@PathVariable Long id) {
        return chatSessionService.closeChatSession(id);
    }
}
