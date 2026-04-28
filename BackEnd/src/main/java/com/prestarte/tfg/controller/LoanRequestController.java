package com.prestarte.tfg.controller;

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
    public LoanRequest createLoanRequest(@RequestBody LoanRequest loanRequest) {
        return loanRequestService.createLoanRequest(loanRequest);
    }

    @GetMapping
    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestService.getAllLoanRequests();
    }

    @GetMapping("/{id}")
    public LoanRequest getLoanRequestById(@PathVariable Long id) {
        return loanRequestService.getLoanRequestById(id)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));
    }
}