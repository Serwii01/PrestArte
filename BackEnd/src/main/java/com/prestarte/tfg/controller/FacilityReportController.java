package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.FacilityReportRequest;
import com.prestarte.tfg.model.dto.FacilityReportResponse;
import com.prestarte.tfg.service.FacilityReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoints del informe de instalaciones ("facility report").
 *
 * La fundación gestiona el suyo en {@code /me}. Los coleccionistas que
 * han recibido una solicitud de una fundación pueden consultar el de
 * esa fundación en {@code /foundation/{id}}.
 */
@RestController
@RequestMapping("/api/facility-report")
@RequiredArgsConstructor
public class FacilityReportController {

    private final FacilityReportService facilityReportService;

    /** Informe de la fundación con sesión; 204 si aún no lo ha rellenado. */
    @GetMapping("/me")
    public ResponseEntity<FacilityReportResponse> getMine() {
        return facilityReportService.getMine()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/me")
    public ResponseEntity<FacilityReportResponse> saveMine(@Valid @RequestBody FacilityReportRequest body) {
        return ResponseEntity.ok(facilityReportService.saveMine(body));
    }

    /** Adjunta el PDF oficial del informe (modelo UKRG, AAM...). */
    @PostMapping(value = "/me/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FacilityReportResponse> uploadDocument(@RequestPart("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(facilityReportService.uploadDocument(file));
    }

    @GetMapping("/foundation/{foundationId}")
    public ResponseEntity<FacilityReportResponse> getForFoundation(@PathVariable Long foundationId) {
        return ResponseEntity.ok(facilityReportService.getForFoundation(foundationId));
    }
}
