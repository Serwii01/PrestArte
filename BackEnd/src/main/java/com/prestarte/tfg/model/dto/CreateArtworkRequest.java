package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.LoanRequirements;
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
    /** Requisitos estructurados de préstamo (clima, luz, seguridad, antelación). */
    private LoanRequirements requirements;

    /** Ciudad o provincia (pública). */
    @Size(max = 150)
    private String location;

    /** Dirección exacta de recogida (privada). */
    @Size(max = 500)
    private String pickupAddress;

    /** Mostrar el nombre del coleccionista en lugar de "Colección particular". */
    private boolean showOwnerName;

    /** Visibilidad en el catálogo. Si no se indica, PUBLIC. */
    private Artwork.Visibility visibility;

    /** Protección patrimonial (BIC, inventariada...). Si no se indica, NONE. */
    private Artwork.HeritageStatus heritageStatus;

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
