package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.ProvenanceEntry;
import jakarta.validation.constraints.*;
import lombok.Data;

/** Nuevo eslabón de procedencia. El justificante opcional viaja como parte {@code file}. */
@Data
public class ProvenanceEntryRequest {

    @NotBlank(message = "Indica el propietario o poseedor")
    @Size(max = 300)
    private String owner;

    @NotNull(message = "Indica desde qué año")
    @Min(value = 0, message = "Año no válido")
    @Max(value = 2100, message = "Año no válido")
    private Integer fromYear;

    /** Vacío si es el propietario actual. */
    @Min(value = 0, message = "Año no válido")
    @Max(value = 2100, message = "Año no válido")
    private Integer toYear;

    @NotNull(message = "Indica cómo la adquirió")
    private ProvenanceEntry.AcquisitionMethod acquisitionMethod;

    @Size(max = 2000)
    private String notes;
}
