package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Hito de supervisión remota tal y como lo ve el frontend. */
@Data
@Builder
public class SupervisionCheckpointResponse {
    private Long id;
    private String stage;
    private String note;
    /** UUIDs de las fotos (requieren sesión). */
    private List<String> photoIds;
    private String authorName;
    private String authorRole;
    private LocalDateTime createdAt;
    private String status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
}
