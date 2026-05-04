package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.service.LoanRequestService;
import com.prestarte.tfg.service.PdfGeneratorService;
import com.prestarte.tfg.service.ShipmentService; // IMPORTANTE: Importa el servicio
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final PdfGeneratorService pdfGeneratorService;
    // LÍNEA AÑADIDA PARA CORREGIR EL ERROR:
    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<LoanResponse> create(@RequestBody CreateLoanRequest dto) {
        return ResponseEntity.ok(loanRequestService.createRequest(dto));
    }

    @GetMapping("/{id}")
    public LoanRequest getLoanById(@PathVariable Long id) {
        return loanRequestService.getById(id);
    }

    /**
     * Endpoint para descargar el contrato de préstamo en PDF.
     * Genera el documento al vuelo incluyendo las condiciones especiales, DNI/CIF y transporte.
     */
    @GetMapping("/{id}/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable Long id) {
        // 1. Obtenemos los datos del préstamo
        LoanRequest loan = loanRequestService.getById(id);

        // 2. Obtenemos los datos del envío asociado (si existe)
        Shipment shipment = null;
        try {
            // Buscamos el envío asociado a este préstamo para los detalles de transporte
            shipment = shipmentService.getByLoanId(id);
        } catch (Exception e) {
            System.out.println("Aviso: Generando contrato sin datos de envío detallados para el ID: " + id);
        }

        // 3. Generamos el PDF usando el diseño profesional
        byte[] pdfBytes = pdfGeneratorService.generateLoanContract(loan, shipment);

        // 4. Configuramos las cabeceras de respuesta
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        String fileName = "Contrato_" + loan.getArtwork().getTitle().replace(" ", "_") + ".pdf";
        headers.setContentDispositionFormData("attachment", fileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<LoanResponse> acceptLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.updateStatus(id, LoanRequest.Status.ACEPTADA));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<LoanResponse> rejectLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanRequestService.updateStatus(id, LoanRequest.Status.RECHAZADA));
    }

    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<List<LoanRequest>> getByCollector(@PathVariable Long collectorId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByCollector(collectorId));
    }
}