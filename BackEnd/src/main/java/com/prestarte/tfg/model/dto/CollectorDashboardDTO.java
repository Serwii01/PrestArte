package com.prestarte.tfg.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Resumen agregado del panel del coleccionista.
 *
 * Agrupa la información que se muestra en la pantalla principal del
 * rol: obras todavía disponibles en la colección, solicitudes que
 * están a la espera de respuesta y préstamos ya en curso.
 */
@Data
@Builder
public class CollectorDashboardDTO {
    /** Obras del coleccionista que no están comprometidas en un préstamo activo. */
    private List<ArtworkDto> availableArtworks;
    /** Solicitudes recibidas que están pendientes de aceptar o rechazar. */
    private List<LoanResponse> pendingLoans;
    /** Envíos relacionados con obras del coleccionista que están en marcha. */
    private List<ShipmentResponse> activeLoans;
}
