package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private String status;
    private String transportCompanyName;
    private String artworkTitle;
    private String receivedBy;  // <--- Nuevo
    private String notes;       // <--- Nuevo
    private LocalDateTime deliveryDate; // <--- Nuevo
    private LocalDateTime createdAt;
    // En tu archivo ShipmentResponse.java
    private LocalDateTime startDate; // Fecha de inicio del préstamo
    private LocalDateTime endDate;   // Fecha de fin del préstamo
}