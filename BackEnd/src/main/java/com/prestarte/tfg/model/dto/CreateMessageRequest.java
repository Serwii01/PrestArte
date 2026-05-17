package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMessageRequest {

    @NotNull(message = "chatSessionId es obligatorio")
    private Long chatSessionId;

    /**
     * Ignorado por el servidor: el remitente se resuelve siempre desde el JWT
     * para evitar suplantación. Se mantiene el campo como opcional por
     * compatibilidad con clientes existentes.
     */
    private Long senderId;

    @NotBlank(message = "El contenido no puede estar vacío")
    private String content;

    private Message.TipoMensaje tipo;
}