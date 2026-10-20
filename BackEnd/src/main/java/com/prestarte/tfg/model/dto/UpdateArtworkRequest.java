package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.LoanRequirements;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload utilizado para actualizar los datos de una obra.
 *
 * Todos los campos son opcionales y solo se aplican los que vienen
 * informados, lo que permite hacer cambios parciales sin tener que
 * reenviar la ficha completa.
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

    @Size(max = 500)
    private String pickupAddress;

    /** Si se envía, sustituye por completo los requisitos anteriores. */
    private LoanRequirements requirements;

    private Boolean showOwnerName;

    private Artwork.Visibility visibility;

    /** Protección patrimonial (BIC, inventariada...). Si no se indica, NONE. */
    private Artwork.HeritageStatus heritageStatus;

    private Long preferredTransportCompanyId;

    private Boolean preferredTransportMandatory;
}
