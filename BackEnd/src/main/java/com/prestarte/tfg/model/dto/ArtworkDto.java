package com.prestarte.tfg.model.dto;

import com.prestarte.tfg.model.entity.LoanRequirements;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO público que representa una obra del catálogo.
 *
 * Refleja los datos descriptivos, físicos y económicos de la obra,
 * la referencia al coleccionista propietario, la empresa de
 * transporte preferida (si la hay) y la lista de archivos asociados
 * filtrada según el usuario que consulta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtworkDto {

    private Long id;
    private String title;
    private String artist;
    private Integer year;
    private Double widthCm;
    private Double heightCm;
    private Double depthCm;
    /** Estado de conservación: EXCELLENT, GOOD, FAIR, POOR o DAMAGED. */
    private String condition;
    private String description;
    /** Valor exacto: solo para el dueño y la administración. */
    private Double estimatedValue;
    /** Horquilla de valor (p. ej. "100.000 – 250.000 €"): para usuarios verificados. */
    private String estimatedValueRange;
    private String loanConditions;
    /** Requisitos estructurados de préstamo; nulo si la obra no tiene ninguno. */
    private LoanRequirements requirements;
    /** Ciudad o provincia (pública). */
    private String location;
    /** Dirección exacta: dueño, administración y transportista con envío en curso. */
    private String pickupAddress;
    /** Nulo para terceros cuando la obra es anónima. */
    private Long collectorId;
    /** "Colección particular" para terceros cuando la obra es anónima. */
    private String collectorName;
    /** Ajustes de privacidad: solo se devuelven al dueño y a la administración. */
    private Boolean showOwnerName;
    private String visibility;
    /** NONE, INVENTORIED o BIC. */
    private String heritageStatus;
    private Long preferredTransportCompanyId;
    private String preferredTransportCompanyName;
    private boolean preferredTransportMandatory;
    private boolean availableForLoan;
    private List<FileDto> files;
    private LocalDateTime createdAt;
}
