package com.prestarte.tfg.model.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Payload utilizado por la fundación para solicitar el préstamo de
 * una obra.
 *
 * Recoge la obra deseada, el rango de fechas y unas condiciones
 * acordadas en texto libre que se trasladarán al contrato si la
 * solicitud avanza.
 */
@Data
public class CreateLoanRequest {
    private Long artworkId;
    private Long foundationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String agreedConditions;
}
