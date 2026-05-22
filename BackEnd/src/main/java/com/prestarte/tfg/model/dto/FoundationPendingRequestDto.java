package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDate;

/**
 * DTO que describe una petición de préstamo pendiente desde el punto
 * de vista de la fundación.
 *
 * Incluye los datos identificativos de la obra y del coleccionista,
 * las fechas solicitadas y las condiciones que el coleccionista
 * exige durante la exposición.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoundationPendingRequestDto {
    private Long loanRequestId;
    private String artworkTitle;
    private String artist;
    private String collectorName;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Condiciones definidas por el coleccionista para el préstamo. */
    private String loanConditions;
    private String status;
}
