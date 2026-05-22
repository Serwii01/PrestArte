package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO que describe una obra recibida y custodiada actualmente por la
 * fundación.
 *
 * Recoge los datos básicos del envío que la trajo: identificador,
 * título de la obra, coleccionista, fecha de entrega y empresa
 * encargada del transporte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoundationInventoryDto {
    private Long shipmentId;
    private String artworkTitle;
    private String collectorName;
    private LocalDateTime deliveryDate;
    private String trackingNumber;
    private String transportCompany;
}
