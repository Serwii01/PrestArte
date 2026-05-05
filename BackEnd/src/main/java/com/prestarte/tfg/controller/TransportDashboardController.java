package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport-dashboard") // Ruta específica para operativa
@RequiredArgsConstructor
public class TransportDashboardController {

    private final ShipmentService shipmentService;

    /**
     * Lista completa de servicios (Histórico y actual).
     */
    @GetMapping("/{companyId}/all-services")
    public ResponseEntity<List<ShipmentResponse>> getAllServices(@PathVariable Long companyId) {
        return ResponseEntity.ok(shipmentService.getByTransportCompany(companyId));
    }

    /**
     * Bandeja de entrada: Solicitudes de presupuesto que aún no han sido atendidas[cite: 7].
     */
    @GetMapping("/{companyId}/new-requests")
    public ResponseEntity<List<ShipmentResponse>> getNewRequests(@PathVariable Long companyId) {
        List<ShipmentResponse> pending = shipmentService.getByTransportCompany(companyId)
                .stream()
                .filter(s -> s.getPrice() == null || s.getPrice() == 0) // Sin presupuesto enviado[cite: 7]
                .toList();
        return ResponseEntity.ok(pending);
    }

    /**
     * En ruta: Servicios aprobados que están en proceso de recogida o tránsito[cite: 7].
     */
    @GetMapping("/{companyId}/active-transits")
    public ResponseEntity<List<ShipmentResponse>> getActiveTransits(@PathVariable Long companyId) {
        List<ShipmentResponse> active = shipmentService.getByTransportCompany(companyId)
                .stream()
                .filter(s -> s.isPriceAccepted() && !"ENTREGADO".equals(s.getStatus())) // Aprobado pero no finalizado
                .toList();
        return ResponseEntity.ok(active);
    }
}