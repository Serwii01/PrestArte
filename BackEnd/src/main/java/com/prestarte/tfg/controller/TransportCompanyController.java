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

/**
 * Endpoints REST relacionados con las empresas de transporte.
 *
 * Expone el listado público de empresas aprobadas y la ficha
 * individual, accesibles sin autenticación, así como la actualización
 * del perfil por parte del titular o un administrador y la
 * eliminación administrativa.
 */
@RestController
@RequestMapping("/api/transport-companies")
@RequiredArgsConstructor
public class TransportCompanyController {

    private final TransportCompanyService transportCompanyService;

    /** Devuelve el listado público de empresas aprobadas. */
    @GetMapping
    public ResponseEntity<List<TransportCompanyProfileDto>> getAll() {
        return ResponseEntity.ok(transportCompanyService.getPublicProfiles());
    }

    /** Devuelve la ficha pública de una empresa concreta. */
    @GetMapping("/{id}")
    public ResponseEntity<TransportCompanyProfileDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transportCompanyService.getPublicProfile(id));
    }

    /** Da de alta una empresa directamente desde una llamada interna. */
    @PostMapping
    public ResponseEntity<TransportCompany> create(@RequestBody TransportCompany company) {
        return ResponseEntity.ok(transportCompanyService.registerCompany(company));
    }

    /** Actualiza el perfil público de la empresa. */
    @PutMapping("/{id}/profile")
    public ResponseEntity<TransportCompanyProfileDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransportCompanyRequest body) {
        return ResponseEntity.ok(transportCompanyService.updateProfile(id, body));
    }

    /** Elimina una empresa de transporte. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transportCompanyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
