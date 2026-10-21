package com.prestarte.tfg.controller;

import com.prestarte.tfg.service.AdminMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores de la red para el panel de administración (rol ADMIN). */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final AdminMetricsService adminMetricsService;

    @GetMapping
    public ResponseEntity<AdminMetricsService.Metrics> metrics() {
        return ResponseEntity.ok(adminMetricsService.metrics());
    }
}
