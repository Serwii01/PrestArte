package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.ChatSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionResponse {

    private Long id;
    private Long loanRequestId;
    private ChatSession.EstadoChat estado;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}