package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Firma de conformidad (o desacuerdo) de la otra parte con un informe de estado. */
@Data
public class SignConditionReportRequest {

    @NotNull(message = "Indica si estás conforme con el informe")
    private Boolean agree;

    /** Obligatorio si no está conforme. */
    @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
    private String comment;
}
