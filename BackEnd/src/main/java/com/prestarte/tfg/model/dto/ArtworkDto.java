package com.prestarte.tfg.model.dto;

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
    private Double estimatedValue;
    private String loanConditions;
    private String location;
    private Long collectorId;
    private String collectorName;
    private Long preferredTransportCompanyId;
    private String preferredTransportCompanyName;
    private boolean preferredTransportMandatory;
    private boolean availableForLoan;
    private List<FileDto> files;
    private LocalDateTime createdAt;
}
