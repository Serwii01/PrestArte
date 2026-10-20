package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Entrada del historial de un préstamo tal y como la ve el frontend. */
@Data
@Builder
public class LoanEventResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String message;
    /** Nombre del usuario que provocó el hito, o null si fue el sistema. */
    private String actorName;
    private String actorRole;
    private LocalDateTime createdAt;
}
