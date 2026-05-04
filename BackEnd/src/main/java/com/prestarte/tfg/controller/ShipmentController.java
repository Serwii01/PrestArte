package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * Crea un envío para un préstamo aceptado (ej: el préstamo ID 3).
     */
    @PostMapping("/create")
    public ResponseEntity<ShipmentResponse> createShipment(
            @RequestParam Long loanId,
            @RequestParam Long transportCompanyId) {
        return ResponseEntity.ok(shipmentService.createShipment(loanId, transportCompanyId));
    }

    /**
     * Lista todos los envíos asignados a una empresa de transporte específica.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ShipmentResponse>> getByCompany(@PathVariable Long companyId) {
        List<ShipmentResponse> responses = shipmentService.getByCompany(companyId)
                .stream()
                .map(s -> ShipmentResponse.builder()
                        .id(s.getId())
                        .trackingNumber(s.getTrackingNumber())
                        .status(s.getStatus().name())
                        .transportCompanyName(s.getTransportCompany().getCompanyName())
                        .artworkTitle(s.getLoanRequest().getArtwork().getTitle())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(responses);
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Shipment.ShipmentStatus status) {
        return ResponseEntity.ok(shipmentService.updateShipmentStatus(id, status));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ShipmentResponse> confirmArrival(
            @PathVariable Long id,
            @RequestBody ConfirmReceiptRequest request) {
        return ResponseEntity.ok(shipmentService.confirmArrival(id, request));
    }
}