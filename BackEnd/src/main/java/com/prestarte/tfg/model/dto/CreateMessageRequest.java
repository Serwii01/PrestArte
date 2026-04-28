package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMessageRequest {

    @NotNull(message = "chatSessionId es obligatorio")
    private Long chatSessionId;

    @NotNull(message = "senderId es obligatorio")
    private Long senderId;

    @NotBlank(message = "El contenido no puede estar vacío")
    private String content;

    private Message.TipoMensaje tipo;
}