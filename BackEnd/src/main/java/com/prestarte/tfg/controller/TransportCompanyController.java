package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.service.TransportCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportCompanyController {

    private final TransportCompanyService transportCompanyService;

    @PostMapping("/register")
    public ResponseEntity<TransportCompany> register(@RequestBody TransportCompany company) {
        return ResponseEntity.ok(transportCompanyService.registerCompany(company));
    }
}