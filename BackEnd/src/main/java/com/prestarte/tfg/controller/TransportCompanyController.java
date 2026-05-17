package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.TransportCompanyProfileDto;
import com.prestarte.tfg.model.dto.UpdateTransportCompanyRequest;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.service.TransportCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport-companies")
@RequiredArgsConstructor
public class TransportCompanyController {

    private final TransportCompanyService transportCompanyService;

    /** Listado público de empresas aprobadas (perfil resumido). */
    @GetMapping
    public ResponseEntity<List<TransportCompanyProfileDto>> getAll() {
        return ResponseEntity.ok(transportCompanyService.getPublicProfiles());
    }

    /** Ficha pública de una empresa concreta. */
    @GetMapping("/{id}")
    public ResponseEntity<TransportCompanyProfileDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transportCompanyService.getPublicProfile(id));
    }

    /** Registro directo (sin verificación KYB). Se mantiene por compatibilidad. */
    @PostMapping
    public ResponseEntity<TransportCompany> create(@RequestBody TransportCompany company) {
        return ResponseEntity.ok(transportCompanyService.registerCompany(company));
    }

    /** El propietario actualiza su perfil público. */
    @PutMapping("/{id}/profile")
    public ResponseEntity<TransportCompanyProfileDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransportCompanyRequest body) {
        return ResponseEntity.ok(transportCompanyService.updateProfile(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transportCompanyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
