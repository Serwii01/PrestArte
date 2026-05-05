package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private String status;
    private String transportCompanyName;
    private String artworkTitle;

    // Datos económicos y seguro
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

    // --- AÑADE ESTOS DOS CAMPOS PARA EL DASHBOARD ---
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}