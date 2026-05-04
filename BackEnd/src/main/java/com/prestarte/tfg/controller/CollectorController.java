package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.CollectorDashboardDTO;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.service.CollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
public class CollectorController {

    private final CollectorService collectorService;

    @PostMapping
    public Collector createCollector(@RequestBody Collector collector) {
        return collectorService.createCollector(collector);
    }

    @GetMapping
    public List<Collector> getAllCollectors() {
        return collectorService.getAllCollectors();
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<CollectorDashboardDTO> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(collectorService.getDashboard(id));
    }
}