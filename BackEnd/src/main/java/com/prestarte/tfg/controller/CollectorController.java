package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CollectorDashboardDTO;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST específicos del rol coleccionista.
 *
 * Permiten consultar el listado de coleccionistas, crear uno desde
 * llamadas internas (utilizadas por las rutinas de inicialización y
 * pruebas) y recuperar el resumen del panel principal.
 */
@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;

    /** Crea un nuevo coleccionista. */
    @PostMapping
    public Collector createCollector(@RequestBody Collector collector) {
        return collectorService.createCollector(collector);
    }

    /** Devuelve el listado completo de coleccionistas. */
    @GetMapping
    public List<Collector> getAllCollectors() {
        return collectorService.getAllCollectors();
    }

    /** Devuelve el resumen del panel del coleccionista indicado. */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<CollectorDashboardDTO> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(collectorService.getDashboard(id));
    }
}
