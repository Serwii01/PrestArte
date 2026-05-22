package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload utilizado para enviar un mensaje a una sesión de chat.
 *
 * El servidor resuelve siempre el emisor a partir del token JWT, por
 * lo que el campo {@code senderId} se conserva únicamente por
 * compatibilidad y no se tiene en cuenta.
 */
@Data
public class CreateMessageRequest {

    @NotNull(message = "chatSessionId es obligatorio")
    private Long chatSessionId;

    /** Campo legado; el remitente se obtiene siempre del token de la petición. */
    private Long senderId;

    @NotBlank(message = "El contenido no puede estar vacío")
    private String content;

    private Message.TipoMensaje tipo;
}
