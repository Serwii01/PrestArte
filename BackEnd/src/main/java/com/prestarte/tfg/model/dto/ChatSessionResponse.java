package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO público que representa una sesión de chat.
 *
 * Incluye los datos identificativos de la sesión y su estado actual
 * (activa o cerrada), suficiente para que el frontend pueda decidir
 * si el usuario puede enviar nuevos mensajes.
 */
@Data
@Builder
public class ChatSessionResponse {

    private Long id;
    private Long loanRequestId;
    private ChatSession.EstadoChat estado;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
