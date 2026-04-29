package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.service.LoanRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    @PostMapping
    public LoanRequest createLoanRequest(@RequestBody CreateLoanRequest request) {
        return loanRequestService.createLoanRequest(request);
    }

    @GetMapping
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestService.getAllLoanRequests();
    }

    @PatchMapping("/{id}/status")
    public LoanRequest updateStatus(
            @PathVariable Long id,
            @RequestParam LoanRequest.Status status) {
        return loanRequestService.updateRequestStatus(id, status);
    }
}