package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta canónica para LoanRequest. Sustituye a la entidad cruda en todos
 * los endpoints REST: evita ciclos de serialización y problemas de lazy.
 */
@Data
@Builder
public class LoanResponse {
    private Long id;

    private Long artworkId;
    private String artworkTitle;
    private String artworkArtist;

    private Long collectorId;
    private String collectorName;

    private Long foundationId;
    private String foundationName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String agreedConditions;

    private String status;

    /** Si es true, el coleccionista exigió específicamente la empresa de transporte. */
    private boolean transportCompanyMandatory;

    /** Id del Shipment OUTBOUND vinculado, o null si aún no se ha creado. */
    private Long shipmentId;

    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
