package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Artwork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    /** Daños señalados sobre las fotos (opcional). */
    @Size(max = 50, message = "Demasiadas marcas")
    private List<@Valid Mark> damageMarks;

    /**
     * Marca de daño. {@code photoIndex} es la posición de la foto en las
     * partes {@code photos} de la petición; x e y, en % (0-100).
     */
    @Data
    public static class Mark {
        @NotNull @Min(0)
        private Integer photoIndex;
        @NotNull @DecimalMin("0") @DecimalMax("100")
        private Double x;
        @NotNull @DecimalMin("0") @DecimalMax("100")
        private Double y;
        @Size(max = 500)
        private String note;
    }
}
