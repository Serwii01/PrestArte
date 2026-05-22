package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO público que representa un préstamo.
 *
 * Se utiliza como respuesta en todos los endpoints relacionados con
 * el flujo de préstamo. Incluye los datos identificativos de la
 * obra, las partes implicadas, el estado actual y las referencias
 * a los envíos asociados cuando ya existen.
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

    /** Indica si la empresa de transporte se ha marcado como obligatoria. */
    private boolean transportCompanyMandatory;

    /** Identificador del envío de ida, si ya está creado. */
    private Long shipmentId;

    /** Identificador del envío de retorno, presente desde el inicio de la devolución. */
    private Long returnShipmentId;

    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
