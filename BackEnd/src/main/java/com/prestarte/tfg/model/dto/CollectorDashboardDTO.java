package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CollectorDashboardDTO {
    private List<ArtworkDto> availableArtworks; // En casa
    private List<LoanResponse> pendingLoans;    // Alguien me ha pedido una obra (esperando respuesta)
    private List<ShipmentResponse> activeLoans; // Obras que ya están prestadas o en camino
}