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
 * Endpoints REST relacionados con los envíos de obras.
 *
 * Igual que en {@link LoanRequestController}, cada endpoint representa
 * una acción concreta del flujo (subir presupuesto, aprobar, recoger,
 * confirmar entrega...). Las transiciones inválidas las rechaza la
 * máquina de estados del envío.
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final PdfGeneratorService pdfGeneratorService;

    // ===== Lecturas =====

    /** Devuelve el detalle de un envío concreto. */
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getById(id));
    }

    /** Devuelve los envíos asignados a una empresa de transporte. */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ShipmentResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(shipmentService.getByTransportCompany(companyId));
    }

    // ===== Acciones del flujo =====

    /** La empresa de transporte sube su presupuesto al envío. */
    @PostMapping("/{id}/quote")
    public ResponseEntity<ShipmentResponse> proposeQuote(@PathVariable Long id,
                                                         @Valid @RequestBody ProposeQuoteRequest body) {
        return ResponseEntity.ok(shipmentService.proposeQuote(id, body));
    }

    /** La fundación aprueba el presupuesto del envío. */
    @PostMapping("/{id}/approve-quote")
    public ResponseEntity<ShipmentResponse> approveQuote(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.approveQuote(id));
    }

    /** La fundación rechaza el presupuesto del envío. */
    @PostMapping("/{id}/reject-quote")
    public ResponseEntity<ShipmentResponse> rejectQuote(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) CancelLoanRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(shipmentService.rejectQuote(id, reason));
    }

    /** La empresa de transporte indica que ha recogido la obra. */
    @PostMapping("/{id}/picked-up")
    public ResponseEntity<ShipmentResponse> markPickedUp(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markPickedUp(id));
    }

    /** La empresa de transporte marca el envío como en tránsito. */
    @PostMapping("/{id}/in-transit")
    public ResponseEntity<ShipmentResponse> markInTransit(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markInTransit(id));
    }

    /** Confirmación de entrega del envío en su destino. */
    @PostMapping("/{id}/delivered")
    public ResponseEntity<ShipmentResponse> confirmDelivery(@PathVariable Long id,
                                                            @Valid @RequestBody ConfirmReceiptRequest body) {
        return ResponseEntity.ok(shipmentService.confirmDelivery(id, body));
    }

    // ===== Descarga del contrato en PDF =====

    /** Devuelve el contrato asociado al envío en formato PDF. */
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
