package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.AcceptLoanRequest;
import com.prestarte.tfg.model.dto.CancelLoanRequest;
import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.service.LoanRequestService;
import com.prestarte.tfg.service.PdfGeneratorService;
import com.prestarte.tfg.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST sobre LoanRequest. Cada endpoint refleja una ACCIÓN del flujo
 * de negocio (no una transición de estado arbitraria), y la state machine
 * impide transiciones inválidas con un 409.
 */
@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final ShipmentService shipmentService;
    private final PdfGeneratorService pdfGeneratorService;

    /* ========== CREACIÓN Y LECTURA ========== */

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody CreateLoanRequest dto) {
        return ResponseEntity.ok(loanRequestService.createRequest(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAll() {
        return ResponseEntity.ok(loanRequestService.getAllLoanRequests());
    }

    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<List<LoanResponse>> getByCollector(@PathVariable Long collectorId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByCollector(collectorId));
    }

    @GetMapping("/foundation/{foundationId}")
    public ResponseEntity<List<LoanResponse>> getByFoundation(@PathVariable Long foundationId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByFoundation(foundationId));
    }

    /* ========== ACCIONES DEL FLUJO ========== */

    /** Coleccionista acepta y elige empresa de transporte. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<LoanResponse> accept(@PathVariable Long id,
                                               @Valid @RequestBody AcceptLoanRequest body) {
        return ResponseEntity.ok(loanRequestService.accept(
                id, body.getTransportCompanyId(), body.isTransportCompanyMandatory()));
    }

    /** Coleccionista rechaza la solicitud (terminal). */
    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.reject(id));
    }

    /** Cualquier parte cancela el préstamo (solo antes de IN_TRANSIT). */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoanResponse> cancel(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) CancelLoanRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(loanRequestService.cancel(id, reason));
    }

    /** Coleccionista confirma que la obra está lista para recoger. */
    @PostMapping("/{id}/ready-for-pickup")
    public ResponseEntity<LoanResponse> markReadyForPickup(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.markReadyForPickup(id));
    }

    /** Museo inicia el retorno de la obra al final del préstamo. */
    @PostMapping("/{id}/start-return")
    public ResponseEntity<LoanResponse> startReturn(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.startReturn(id));
    }

    /** Coleccionista confirma que la obra ha vuelto a sus manos (cierre del ciclo). */
    @PostMapping("/{id}/complete-return")
    public ResponseEntity<LoanResponse> completeReturn(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.completeReturn(id));
    }

    /* ========== CONTRATO PDF ========== */

    @GetMapping("/{id}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
        // Necesitamos la entidad cruda para que el PDF tenga acceso a todos los campos
        LoanRequest loan = loanRequestService.getEntityById(id);
        Shipment shipment = shipmentService.getByLoanId(id);

        byte[] pdfBytes = pdfGeneratorService.generateLoanContract(loan, shipment);

        String fileName = "Contrato_" + loan.getArtwork().getTitle().replace(" ", "_") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
