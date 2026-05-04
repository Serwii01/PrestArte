package com.prestarte.tfg.model.dto;

import lombok.*;
import java.time.LocalDateTime;

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