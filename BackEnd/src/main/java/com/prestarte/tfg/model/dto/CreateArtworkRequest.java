package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Payload utilizado para dar de alta una obra en el catálogo.
 *
 * Recoge los datos descriptivos, físicos y económicos de la obra,
 * además de las condiciones que el coleccionista exige para el
 * préstamo y la referencia opcional a su empresa de transporte
 * preferida.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateArtworkRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "El nombre del artista es obligatorio")
    @Size(max = 200)
    private String artist;

    @Min(value = 0, message = "El año no puede ser negativo")
    private Integer year;

    @PositiveOrZero(message = "Las dimensiones no pueden ser negativas")
    private Double widthCm;

    @PositiveOrZero(message = "Las dimensiones no pueden ser negativas")
    private Double heightCm;

    @PositiveOrZero(message = "Las dimensiones no pueden ser negativas")
    private Double depthCm;

    /** Estado de conservación: EXCELLENT, GOOD, FAIR, POOR o DAMAGED. */
    @NotBlank(message = "El estado de conservación es obligatorio")
    @Pattern(regexp = "EXCELLENT|GOOD|FAIR|POOR|DAMAGED",
            message = "Estado de conservación inválido")
    private String condition;

    private String description;

    /** Valor estimado en euros, utilizado como referencia del seguro. */
    @NotNull(message = "El valor estimado es obligatorio")
    @Positive(message = "El valor estimado debe ser mayor que 0")
    private Double estimatedValue;

    /** Condiciones que el coleccionista exige al museo (clima, luz, manipulación...). */
    private String loanConditions;

    /** Ciudad o localidad donde se encuentra físicamente la obra. */
    @Size(max = 150)
    private String location;

    /** Empresa de transporte preferida por el coleccionista, si la hay. */
    private Long preferredTransportCompanyId;

    /**
     * Indica si la empresa preferida es obligatoria. Cuando es true,
     * un rechazo posterior del presupuesto cancela el préstamo.
     */
    private boolean preferredTransportMandatory;

    @NotNull(message = "El coleccionista es obligatorio")
    private Long collectorId;
}
