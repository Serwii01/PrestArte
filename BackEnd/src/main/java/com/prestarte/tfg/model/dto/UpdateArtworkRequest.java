package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Body para PUT /api/artworks/{id}. Solo el dueño (o admin) puede llamar.
 * Todos los campos son opcionales: se actualizan los que vengan.
 */
@Data
public class UpdateArtworkRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String artist;

    @Min(value = 0)
    private Integer year;

    @PositiveOrZero
    private Double widthCm;

    @PositiveOrZero
    private Double heightCm;

    @PositiveOrZero
    private Double depthCm;

    @Pattern(regexp = "EXCELLENT|GOOD|FAIR|POOR|DAMAGED",
            message = "Estado de conservación inválido")
    private String condition;

    private String description;

    @Positive
    private Double estimatedValue;

    private String loanConditions;

    @Size(max = 150)
    private String location;

    private Long preferredTransportCompanyId;

    private Boolean preferredTransportMandatory;
}
