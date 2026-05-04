package com.prestarte.tfg.model.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoundationDashboardDto {
    private List<FoundationPendingRequestDto> pendingRequests;
    private List<FoundationInventoryDto> activeInventory;
}