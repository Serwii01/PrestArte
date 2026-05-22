package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO público que representa un envío.
 *
 * Incluye la información necesaria para mostrar el detalle del envío
 * (datos del préstamo, de la empresa de transporte, importe,
 * seguro y estado actual) y los datos finales de entrega cuando ya se
 * ha confirmado la recepción.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private Long loanRequestId;
    private Long transportCompanyId;
    private String trackingNumber;
    private String status;
    private String direction;
    private String transportCompanyName;
    private String artworkTitle;

    // Datos económicos y de seguro
    private Double price;
    private Double insuranceCost;
    private Double insuranceValue;
    private String insurancePolicy;
    private boolean priceAccepted;

    // Datos de entrega
    private String receivedBy;
    private String notes;
    private LocalDateTime deliveryDate;
    private LocalDateTime createdAt;

    // Fechas del préstamo asociado, mostradas en el panel de la empresa
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
