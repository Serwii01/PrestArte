package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.FoundationDashboardDto;
import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.service.FoundationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foundations")
@RequiredArgsConstructor
public class FoundationController {

    private final FoundationService foundationService;

    @PostMapping
    public Foundation createFoundation(@RequestBody Foundation foundation) {
        return foundationService.createFoundation(foundation);
    }

    @GetMapping
    public List<Foundation> getAllFoundations() {
        return foundationService.getAllFoundations();
    }

    /**
     * Nuevo Endpoint: Dashboard del Museo/Fundación.
     * Devuelve las peticiones pendientes (con sus condiciones) y el inventario activo.
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<FoundationDashboardDto> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(foundationService.getDashboard(id));
    }
}