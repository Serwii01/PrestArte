package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Informe de instalaciones tal y como lo ve el frontend. */
@Data
@Builder
public class FacilityReportResponse {
    private Long foundationId;
    private String foundationName;

    private Boolean climateControlled;
    private Double tempMin;
    private Double tempMax;
    private Double rhMin;
    private Double rhMax;
    private Integer minAchievableLux;
    private Boolean uvFiltered;
    private Boolean guards24h;
    private Boolean cctv;
    private Boolean intrusionAlarm;
    private Boolean fireDetection;
    private String fireSuppression;
    private Boolean displayCasesAvailable;
    private Double accessWidthCm;
    private Double accessHeightCm;
    private Boolean loadingDock;
    private String notes;

    /** PDF oficial adjunto (se descarga con sesión por GET /api/files/{id}). */
    private String documentFileId;
    private String documentFileName;

    private LocalDateTime updatedAt;
    /** False si el informe tiene más de 12 meses. */
    private boolean current;
}
