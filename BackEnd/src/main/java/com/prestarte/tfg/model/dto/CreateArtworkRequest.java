package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

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

    /** Valores válidos: EXCELLENT, GOOD, FAIR, POOR, DAMAGED. */
    @NotBlank(message = "El estado de conservación es obligatorio")
    @Pattern(regexp = "EXCELLENT|GOOD|FAIR|POOR|DAMAGED",
            message = "Estado de conservación inválido")
    private String condition;

    private String description;

    /** Valor estimado en euros. Obligatorio (lo usa el seguro). */
    @NotNull(message = "El valor estimado es obligatorio")
    @Positive(message = "El valor estimado debe ser mayor que 0")
    private Double estimatedValue;

    /** Condiciones que el coleccionista exige al museo (luz, humedad, etc.). */
    private String loanConditions;

    /** Ciudad / pueblo donde se encuentra físicamente la obra. */
    @Size(max = 150)
    private String location;

    /** ID de la empresa de transporte preferida (opcional). */
    private Long preferredTransportCompanyId;

    /**
     * Si true, la empresa preferida es OBLIGATORIA: el museo no podrá negociar
     * con otra. Si la rechaza, el préstamo se cancela.
     */
    private boolean preferredTransportMandatory;

    @NotNull(message = "El coleccionista es obligatorio")
    private Long collectorId;
}
