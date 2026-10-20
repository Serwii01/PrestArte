package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Informe de estado tal y como lo ve el frontend. */
@Data
@Builder
public class ConditionReportResponse {
    private Long id;
    private Long shipmentId;
    /** OUTBOUND (ida) o RETURN (vuelta). */
    private String direction;
    /** PICKUP (recogida) o DELIVERY (entrega). */
    private String stage;
    private String condition;
    private boolean damageReported;
    private String notes;
    private String authorName;
    private String authorRole;
    /** UUIDs de las fotos; se descargan por GET /api/files/{id} con sesión. */
    private List<String> photoIds;
    private LocalDateTime createdAt;
}
