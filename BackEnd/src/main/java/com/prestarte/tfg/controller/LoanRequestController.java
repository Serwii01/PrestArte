package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.service.LoanRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    /**
     * Endpoint para que un Museo/Fundación solicite un préstamo.
     * Solo funcionará si la fundación está aprobada (status: APPROVED).
     */
    @PostMapping("/create")
    public ResponseEntity<LoanResponse> createLoan(@RequestBody CreateLoanRequest dto) {
        return ResponseEntity.ok(loanRequestService.createRequest(dto));
    }

    /**
     * Endpoint para que un Museo vea todas las solicitudes que ha enviado.
     */
    @GetMapping("/foundation/{foundationId}")
    public ResponseEntity<List<LoanRequest>> getRequestsByFoundation(@PathVariable Long foundationId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByFoundation(foundationId));
    }

    /**
     * Endpoint para que un Coleccionista vea las solicitudes que ha recibido de sus obras.
     */
    @GetMapping("/collector/{collectorId}")
    public ResponseEntity<List<LoanRequest>> getRequestsByCollector(@PathVariable Long collectorId) {
        return ResponseEntity.ok(loanRequestService.getRequestsByCollector(collectorId));
    }

    /**
     * Endpoint para que el dueño de la obra (Coleccionista) acepte o rechace la solicitud.
     */
    @PatchMapping("/{loanId}/status")
    public ResponseEntity<LoanResponse> updateStatus(
            @PathVariable Long loanId,
            @RequestParam LoanRequest.Status status) {
        return ResponseEntity.ok(loanRequestService.updateStatus(loanId, status));
    }
}