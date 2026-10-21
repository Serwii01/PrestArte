package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Obra pendiente de diligencia debida, para el panel de administración. */
@Data
@Builder
public class DueDiligencePendingItem {
    private Long artworkId;
    private String title;
    private String artist;
    private Integer year;
    private String collectorName;
    private LocalDateTime titleDeclaredAt;
    private long provenanceEntries;
    private int provenanceGaps;
    private boolean criticalGap;
    private LocalDateTime createdAt;
}
