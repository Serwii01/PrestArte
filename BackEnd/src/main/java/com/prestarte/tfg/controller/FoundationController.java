package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.FoundationDashboardDto;
import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.service.FoundationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST específicos del rol fundación.
 *
 * Permiten crear y listar fundaciones y recuperar el resumen del
 * panel principal, donde aparecen tanto las peticiones pendientes
 * como las obras que la fundación ya ha recibido.
 */
@RestController
@RequestMapping("/api/foundations")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    /** Crea una nueva fundación. */
    @PostMapping
    public Foundation createFoundation(@RequestBody Foundation foundation) {
        return foundationService.createFoundation(foundation);
    }

    /** Devuelve el listado completo de fundaciones registradas. */
    @GetMapping
    public List<Foundation> getAllFoundations() {
        return foundationService.getAllFoundations();
    }

    /**
     * Devuelve el resumen del panel de la fundación: peticiones
     * pendientes con sus condiciones de préstamo e inventario activo
     * en el museo.
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<FoundationDashboardDto> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(foundationService.getDashboard(id));
    }
}
