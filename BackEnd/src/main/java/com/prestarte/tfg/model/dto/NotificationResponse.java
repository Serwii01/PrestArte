package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Aviso tal y como lo ve el frontend. */
@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
}
