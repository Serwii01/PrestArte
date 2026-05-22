package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST que alimentan el panel operativo de las empresas de
 * transporte.
 *
 * Agrupan los envíos asignados a una empresa en tres listas útiles
 * para su trabajo diario: histórico completo, solicitudes nuevas a la
 * espera de presupuesto y servicios ya aprobados que se encuentran
 * activos.
 */
@RestController
@RequestMapping("/api/transport-dashboard")
@RequiredArgsConstructor
public class TransportDashboardController {

    private final ShipmentService shipmentService;

    /** Devuelve todos los servicios asignados a la empresa indicada. */
    @GetMapping("/{companyId}/all-services")
    public ResponseEntity<List<ShipmentResponse>> getAllServices(@PathVariable Long companyId) {
        return ResponseEntity.ok(shipmentService.getByTransportCompany(companyId));
    }

    /** Devuelve las solicitudes que aún no tienen presupuesto. */
    @GetMapping("/{companyId}/new-requests")
    public ResponseEntity<List<ShipmentResponse>> getNewRequests(@PathVariable Long companyId) {
        List<ShipmentResponse> pending = shipmentService.getByTransportCompany(companyId)
                .stream()
                .filter(s -> s.getPrice() == null || s.getPrice() == 0)
                .toList();
        return ResponseEntity.ok(pending);
    }

    /** Devuelve los servicios aprobados que se encuentran en curso. */
    @GetMapping("/{companyId}/active-transits")
    public ResponseEntity<List<ShipmentResponse>> getActiveTransits(@PathVariable Long companyId) {
        List<ShipmentResponse> active = shipmentService.getByTransportCompany(companyId)
                .stream()
                .filter(s -> s.isPriceAccepted() && !"DELIVERED".equals(s.getStatus()))
                .toList();
        return ResponseEntity.ok(active);
    }
}
