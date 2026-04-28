package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long chatSessionId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private Message.TipoMensaje tipo;
}