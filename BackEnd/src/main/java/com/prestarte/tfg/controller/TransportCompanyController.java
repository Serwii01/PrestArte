package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.service.TransportCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport-companies")
@RequiredArgsConstructor
public class TransportCompanyController {

    private final TransportCompanyService transportCompanyService;

    /**
     * Registro de una nueva empresa de transporte.
     */
    @PostMapping
    public ResponseEntity<TransportCompany> create(@RequestBody TransportCompany company) {
        return ResponseEntity.ok(transportCompanyService.registerCompany(company));
    }

    /**
     * Obtener el listado de todas las empresas registradas.
     */
    @GetMapping
    public ResponseEntity<List<TransportCompany>> getAll() {
        return ResponseEntity.ok(transportCompanyService.getAllCompanies());
    }

    /**
     * Obtener los detalles de una empresa específica por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransportCompany> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transportCompanyService.getById(id));
    }

    /**
     * Actualizar los datos de una empresa (email, teléfono, área de cobertura, etc.)[cite: 8].
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransportCompany> update(
            @PathVariable Long id,
            @RequestBody TransportCompany companyDetails) {
        return ResponseEntity.ok(transportCompanyService.updateCompany(id, companyDetails));
    }

    /**
     * Eliminar (o desactivar) una empresa del sistema.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transportCompanyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}