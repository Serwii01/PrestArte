package com.prestarte.tfg.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "La obra es obligatoria")
    private Long artworkId;

    @NotNull(message = "La fundación es obligatoria")
    private Long foundationId;

    /** Exposición para la que se pide la obra. */
    @NotNull(message = "Indica para qué exposición pides la obra")
    private Long exhibitionId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser anterior a hoy")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @Size(max = 5000, message = "Las condiciones no pueden superar 5000 caracteres")
    private String agreedConditions;

    /** La fecha de fin debe ser posterior a la de inicio. */
    @JsonIgnore
    @AssertTrue(message = "La fecha de fin debe ser posterior a la de inicio")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
