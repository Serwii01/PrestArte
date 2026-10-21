package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.DueDiligencePendingItem;
import com.prestarte.tfg.model.dto.DueDiligenceRequest;
import com.prestarte.tfg.model.dto.ProvenanceEntryRequest;
import com.prestarte.tfg.model.dto.ProvenanceResponse;
import com.prestarte.tfg.model.dto.StolenArtCheckResponse;
import com.prestarte.tfg.service.ProvenanceService;
import com.prestarte.tfg.service.StolenArtRegistries;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Titularidad, procedencia y diligencia debida de las obras.
 *
 * Las rutas bajo {@code /api/admin} quedan reservadas al rol ADMIN por
 * {@code SecurityConfig}; el resto exige sesión.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProvenanceController {

    private final ProvenanceService provenanceService;
    private final StolenArtRegistries stolenArtRegistries;

    @GetMapping("/artworks/{id}/provenance")
    public ResponseEntity<ProvenanceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(provenanceService.get(id));
    }

    /** El coleccionista firma la declaración de titularidad. */
    @PostMapping("/artworks/{id}/title-declaration")
    public ResponseEntity<ProvenanceResponse> declareTitle(@PathVariable Long id) {
        return ResponseEntity.ok(provenanceService.declareTitle(id));
    }

    /** Añade un eslabón: parte {@code data} (JSON) y parte {@code file} opcional (justificante). */
    @PostMapping(value = "/artworks/{id}/provenance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProvenanceResponse> addEntry(
            @PathVariable Long id,
            @Valid @RequestPart("data") ProvenanceEntryRequest data,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ResponseEntity.ok(provenanceService.addEntry(id, data, file));
    }

    @DeleteMapping("/artworks/{id}/provenance/{entryId}")
    public ResponseEntity<ProvenanceResponse> deleteEntry(@PathVariable Long id, @PathVariable Long entryId) {
        return ResponseEntity.ok(provenanceService.deleteEntry(id, entryId));
    }

    // ===== Administración =====

    @GetMapping("/admin/due-diligence/pending")
    public ResponseEntity<List<DueDiligencePendingItem>> pending() {
        return ResponseEntity.ok(provenanceService.pending());
    }

    @PostMapping("/admin/artworks/{id}/due-diligence")
    public ResponseEntity<ProvenanceResponse> recordDueDiligence(@PathVariable Long id,
                                                                 @Valid @RequestBody DueDiligenceRequest body) {
        return ResponseEntity.ok(provenanceService.recordDueDiligence(id, body));
    }

    /** Bases de obras robadas que consultar para la obra, con los datos para buscarla. */
    @GetMapping("/admin/artworks/{id}/stolen-art-registries")
    public ResponseEntity<StolenArtCheckResponse> stolenArtRegistries(@PathVariable Long id) {
        return ResponseEntity.ok(stolenArtRegistries.checkFor(id));
    }
}
