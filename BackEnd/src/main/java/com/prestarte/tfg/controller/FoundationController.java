package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.FoundationDashboardDto;
import com.prestarte.tfg.service.FoundationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints REST específicos del rol fundación.
 *
 * Solo expone el resumen del panel principal (peticiones pendientes y
 * obras ya recibidas), accesible para la propia fundación y la
 * administración. Las cuentas se crean siempre por el registro
 * verificado ({@code /api/auth/register}).
 */
@RestController
@RequestMapping("/api/foundations")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    /** Resumen del panel de la fundación indicada. */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<FoundationDashboardDto> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(foundationService.getDashboard(id));
    }
}
