package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.service.PdfGeneratorService;
import com.prestarte.tfg.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final PdfGeneratorService pdfGeneratorService;

    /**
     * 1. Solicitar envío (El Museo o Coleccionista elige empresa)
     */
    @PostMapping("/request")
    public ResponseEntity<ShipmentResponse> requestShipment(
            @RequestParam Long loanId,
            @RequestParam Long transportCompanyId) {
        // Usamos el método que renombramos en el service para ser más precisos
        return ResponseEntity.ok(shipmentService.requestShipment(loanId, transportCompanyId));
    }

    /**
     * 2. Proponer presupuesto (Acción del Transportista)
     */
    @PatchMapping("/{id}/propose-budget")
    public ResponseEntity<ShipmentResponse> proposeBudget(
            @PathVariable Long id,
            @RequestParam Double price,
            @RequestParam Double insuranceCost,
            @RequestParam String policy) {
        return ResponseEntity.ok(shipmentService.proposeBudget(id, price, insuranceCost, policy));
    }

    /**
     * 3. Aprobar presupuesto y formalizar (Acción de la Fundación)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ShipmentResponse> approveBudget(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.approveBudget(id));
    }

    /**
     * 4. Descargar Contrato Formal (PDF)
     */
    @GetMapping("/{id}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
        Shipment shipment = shipmentService.getByIdRaw(id); // Necesitarás este pequeño helper en el service

        byte[] pdfContent = pdfGeneratorService.generateLoanContract(
                shipment.getLoanRequest(),
                shipment
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "Contrato_PrestArte_" + shipment.getTrackingNumber() + ".pdf";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    /**
     * 5. Actualizar estado de tránsito (Acción del Transportista)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Shipment.ShipmentStatus status) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, status));
    }

    /**
     * 6. Confirmar llegada (Acción del Museo)
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ShipmentResponse> confirmArrival(
            @PathVariable Long id,
            @RequestBody ConfirmReceiptRequest request) {
        return ResponseEntity.ok(shipmentService.confirmArrival(id, request));
    }

    /**
     * Listados
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ShipmentResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(shipmentService.getByTransportCompany(companyId));
    }
}