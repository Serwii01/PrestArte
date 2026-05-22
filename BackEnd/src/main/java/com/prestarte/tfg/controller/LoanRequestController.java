package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.AcceptLoanRequest;
import com.prestarte.tfg.model.dto.CancelLoanRequest;
import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.dto.ReassignTransportRequest;
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
 * Endpoints REST relacionados con las solicitudes de préstamo.
 *
 * Cada endpoint refleja una acción concreta del flujo de negocio (no
 * una transición arbitraria del estado). La validez de cada
 * transición la asegura la máquina de estados del préstamo, que
 * rechaza con HTTP 409 cualquier intento fuera de las transiciones
 * permitidas.
 */
@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final ShipmentService shipmentService;
    private final PdfGeneratorService pdfGeneratorService;

    // ===== Creación y lectura =====

    /** Crea una nueva solicitud de préstamo desde la fundación. */
    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody CreateLoanRequest dto) {
        return ResponseEntity.ok(loanRequestService.createRequest(dto));
    }

    /** Devuelve el detalle de un préstamo concreto. */
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.getById(id));
    }

    /** Devuelve todos los préstamos del sistema (reservado al administrador). */
    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAll() {
        return ResponseEntity.ok(loanRequestService.getAllLoanRequests());
    }

    /** Devuelve los préstamos asociados a las obras de un coleccionista. */
    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<List<LoanResponse>> getByCollector(@PathVariable Long collectorId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByCollector(collectorId));
    }

    /** Devuelve los préstamos solicitados por una fundación. */
    @GetMapping("/foundation/{foundationId}")
    public ResponseEntity<List<LoanResponse>> getByFoundation(@PathVariable Long foundationId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByFoundation(foundationId));
    }

    // ===== Acciones del flujo =====

    /** El coleccionista acepta la solicitud y selecciona la empresa de transporte. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<LoanResponse> accept(@PathVariable Long id,
                                               @Valid @RequestBody AcceptLoanRequest body) {
        return ResponseEntity.ok(loanRequestService.accept(
                id, body.getTransportCompanyId(), body.isTransportCompanyMandatory()));
    }

    /** El coleccionista rechaza la solicitud de forma definitiva. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.reject(id));
    }

    /**
     * Asigna una empresa de transporte distinta cuando se ha rechazado
     * el presupuesto anterior y la empresa inicial no era obligatoria.
     */
    @PostMapping("/{id}/reassign-transport")
    public ResponseEntity<LoanResponse> reassignTransport(@PathVariable Long id,
                                                          @Valid @RequestBody ReassignTransportRequest body) {
        return ResponseEntity.ok(loanRequestService.reassignTransport(id, body.getTransportCompanyId()));
    }

    /** Cancela el préstamo desde cualquiera de las dos partes implicadas. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoanResponse> cancel(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) CancelLoanRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(loanRequestService.cancel(id, reason));
    }

    /** El coleccionista indica que la obra está preparada para ser recogida. */
    @PostMapping("/{id}/ready-for-pickup")
    public ResponseEntity<LoanResponse> markReadyForPickup(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.markReadyForPickup(id));
    }

    /** La fundación inicia el retorno de la obra al final del préstamo. */
    @PostMapping("/{id}/start-return")
    public ResponseEntity<LoanResponse> startReturn(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.startReturn(id));
    }

    // ===== Descarga del contrato en PDF =====

    /** Devuelve el contrato del préstamo en formato PDF descargable. */
    @GetMapping("/{id}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
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
