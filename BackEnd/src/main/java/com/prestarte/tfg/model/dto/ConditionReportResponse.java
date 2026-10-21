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

    /** Daños señalados sobre las fotos. */
    private List<Mark> marks;

    /** Rol de la parte que debe firmar la conformidad (COLLECTOR, FOUNDATION o TRANSPORT). */
    private String counterpartRole;
    /** True si quien consulta es esa parte y aún no ha firmado. */
    private boolean canSign;
    private String acknowledgedByName;
    private LocalDateTime acknowledgedAt;
    /** True si la otra parte firmó que no está conforme. */
    private Boolean disputed;
    private String acknowledgementComment;

    public record Mark(String photoId, double x, double y, String note) {
    }
}
