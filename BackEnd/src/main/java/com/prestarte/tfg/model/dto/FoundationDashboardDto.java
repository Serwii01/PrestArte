package com.prestarte.tfg.model.dto;

import lombok.*;
import java.util.List;

/**
 * Resumen agregado del panel de la fundación.
 *
 * Reúne las peticiones que la fundación ha enviado y aún están a la
 * espera de respuesta y las obras que ya tiene físicamente en su
 * inventario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoundationDashboardDto {
    private List<FoundationPendingRequestDto> pendingRequests;
    private List<FoundationInventoryDto> activeInventory;
}
