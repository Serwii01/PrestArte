package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ExhibitionCompatibility;
import com.prestarte.tfg.model.dto.ExhibitionRequest;
import com.prestarte.tfg.model.dto.ExhibitionResponse;
import com.prestarte.tfg.service.ExhibitionCompatibilityService;
import com.prestarte.tfg.service.ExhibitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de las exposiciones que organizan las fundaciones. */
@RestController
@RequestMapping("/api/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;
    private final ExhibitionCompatibilityService exhibitionCompatibilityService;

    @PostMapping
    public ResponseEntity<ExhibitionResponse> create(@Valid @RequestBody ExhibitionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exhibitionService.create(body));
    }

    /** Exposiciones de la fundación con sesión, con el estado de sus solicitudes. */
    @GetMapping("/mine")
    public ResponseEntity<List<ExhibitionResponse>> listMine() {
        return ResponseEntity.ok(exhibitionService.listMine());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExhibitionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(exhibitionService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExhibitionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ExhibitionRequest body) {
        return ResponseEntity.ok(exhibitionService.update(id, body));
    }

    /** Compatibilidad de todas las obras de la exposición con las instalaciones (solo organizadora). */
    @GetMapping("/{id}/compatibility")
    public ResponseEntity<ExhibitionCompatibility> compatibility(@PathVariable Long id) {
        return ResponseEntity.ok(exhibitionCompatibilityService.check(id));
    }
}
