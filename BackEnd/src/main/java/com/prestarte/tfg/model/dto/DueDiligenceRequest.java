package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Artwork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Resultado de una comprobación de diligencia debida (solo administración). */
@Data
public class DueDiligenceRequest {

    /** CLEAR o FLAGGED. */
    @NotNull(message = "Indica el resultado")
    private Artwork.DueDiligenceStatus result;

    @NotBlank(message = "Indica qué bases de datos se han consultado")
    @Size(max = 300)
    private String sources;

    @Size(max = 100)
    private String reference;

    @Size(max = 5000)
    private String notes;
}
