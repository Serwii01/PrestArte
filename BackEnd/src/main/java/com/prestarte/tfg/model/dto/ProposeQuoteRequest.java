package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body para POST /api/shipments/{id}/quote.
 * El transportista sube un presupuesto con coste de transporte y de seguro.
 */
@Data
public class ProposeQuoteRequest {

    @NotNull(message = "El precio del transporte es obligatorio")
    @Positive(message = "El precio debe ser mayor que 0")
    private Double price;

    @NotNull(message = "El coste del seguro es obligatorio")
    @PositiveOrZero(message = "El coste del seguro no puede ser negativo")
    private Double insuranceCost;

    @Size(max = 100)
    private String insurancePolicy;
}
