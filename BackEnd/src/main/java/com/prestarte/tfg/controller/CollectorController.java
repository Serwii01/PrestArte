package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CollectorDashboardDTO;
import com.prestarte.tfg.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST específicos del rol coleccionista.
 *
 * Solo expone el resumen del panel principal, accesible para el propio
 * coleccionista y la administración. Las cuentas se crean siempre por
 * el registro ({@code /api/auth/register}), que exige verificación; no
 * hay un alta directa ni un listado de coleccionistas, porque expondría
 * sus datos personales (email, teléfono, DNI) a cualquier usuario.
 */
@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;

    /** Resumen del panel del coleccionista indicado. */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<CollectorDashboardDTO> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(collectorService.getDashboard(id));
    }
}
