package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Artwork;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos del informe de estado que acompañan a la recogida y a la
 * entrega de un envío. Las fotografías viajan aparte, como partes
 * {@code photos} de la petición multipart.
 */
@Data
public class ConditionReportRequest {

    @NotNull(message = "Indica el estado de conservación de la obra")
    private Artwork.Condition condition;

    /** Marca explícita de daños o incidencias. */
    private boolean damageReported;

    @Size(max = 5000, message = "Las observaciones no pueden superar 5000 caracteres")
    private String notes;

    /** Solo en la entrega: persona que recibe la obra. */
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String receivedBy;
}
