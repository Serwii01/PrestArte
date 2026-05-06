package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CancelLoanRequest;
import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ProposeQuoteRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.service.PdfGeneratorService;
import com.prestarte.tfg.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints sobre Shipment. Mismo principio que LoanRequestController:
 * cada endpoint refleja una acción del flujo, no una transición arbitraria.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final PdfGeneratorService pdfGeneratorService;

    /* ========== LECTURA ========== */

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ShipmentResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(shipmentService.getByTransportCompany(companyId));
    }

    /* ========== ACCIONES DEL FLUJO ========== */

    /** Transportista propone presupuesto. REQUESTED → QUOTED. */
    @PostMapping("/{id}/quote")
    public ResponseEntity<ShipmentResponse> proposeQuote(@PathVariable Long id,
                                                         @Valid @RequestBody ProposeQuoteRequest body) {
        return ResponseEntity.ok(shipmentService.proposeQuote(id, body));
    }

    /** Museo aprueba el presupuesto. QUOTED → APPROVED. */
    @PostMapping("/{id}/approve-quote")
    public ResponseEntity<ShipmentResponse> approveQuote(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.approveQuote(id));
    }

    /** Museo rechaza el presupuesto. QUOTED → REJECTED. */
    @PostMapping("/{id}/reject-quote")
    public ResponseEntity<ShipmentResponse> rejectQuote(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) CancelLoanRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(shipmentService.rejectQuote(id, reason));
    }

    /** Transportista marca recogida. APPROVED → PICKED_UP. */
    @PostMapping("/{id}/picked-up")
    public ResponseEntity<ShipmentResponse> markPickedUp(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markPickedUp(id));
    }

    /** Transportista marca en tránsito. PICKED_UP → IN_TRANSIT. */
    @PostMapping("/{id}/in-transit")
    public ResponseEntity<ShipmentResponse> markInTransit(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markInTransit(id));
    }

    /** Museo confirma llegada. IN_TRANSIT → DELIVERED. */
    @PostMapping("/{id}/delivered")
    public ResponseEntity<ShipmentResponse> confirmDelivery(@PathVariable Long id,
                                                            @Valid @RequestBody ConfirmReceiptRequest body) {
        return ResponseEntity.ok(shipmentService.confirmDelivery(id, body));
    }

    /* ========== CONTRATO PDF ========== */

    @GetMapping("/{id}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
        Shipment shipment = shipmentService.getByIdRaw(id);
        byte[] pdf = pdfGeneratorService.generateLoanContract(shipment.getLoanRequest(), shipment);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "Contrato_PrestArte_" + shipment.getTrackingNumber() + ".pdf";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
