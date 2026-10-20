package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CompatibilityReport;
import com.prestarte.tfg.model.dto.CostEstimate;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.UpdateArtworkRequest;
import com.prestarte.tfg.service.ArtworkService;
import com.prestarte.tfg.service.CostEstimateService;
import com.prestarte.tfg.service.FacilityReportService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints REST relacionados con las obras del catálogo.
 *
 * Cubre la consulta pública del catálogo y la ficha de cada obra, las
 * operaciones de mantenimiento que solo puede realizar el
 * coleccionista dueño (o un administrador) y la gestión de los
 * documentos adjuntos a la obra.
 */
@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;
    private final FacilityReportService facilityReportService;
    private final CostEstimateService costEstimateService;

    /** Da de alta una nueva obra en el catálogo. */
    @PostMapping
    public ResponseEntity<ArtworkDto> createArtwork(@Valid @RequestBody CreateArtworkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artworkService.createArtwork(request));
    }

    /** Devuelve el catálogo completo de obras. */
    @GetMapping
    public List<ArtworkDto> getAllArtworks() {
        return artworkService.getAllArtworks();
    }

    /** Devuelve la ficha de una obra. */
    @GetMapping("/{id}")
    public ArtworkDto getArtworkById(@PathVariable Long id) {
        return artworkService.getArtworkById(id);
    }

    /**
     * Compatibilidad de la obra con las instalaciones de la fundación con
     * sesión. {@code startDate} (opcional) permite comprobar la antelación.
     */
    @GetMapping("/{id}/compatibility")
    public CompatibilityReport getCompatibility(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return facilityReportService.checkArtworkForMe(id, startDate);
    }

    /**
     * Coste y huella de carbono estimados de pedir la obra para una
     * exposición de la fundación con sesión. {@code transportCompanyId}
     * es opcional: sin él se usa el transportista preferido de la obra o
     * la tarifa de referencia.
     */
    @GetMapping("/{id}/cost-estimate")
    public CostEstimate getCostEstimate(@PathVariable Long id,
                                        @RequestParam Long exhibitionId,
                                        @RequestParam(required = false) Long transportCompanyId) {
        return costEstimateService.forRequest(id, exhibitionId, transportCompanyId);
    }

    /** Devuelve todas las obras de un coleccionista concreto. */
    @GetMapping("/collector/{collectorId}")
    public List<ArtworkDto> getArtworksByCollector(@PathVariable Long collectorId) {
        return artworkService.getArtworksByCollector(collectorId);
    }

    /** Edita los campos de una obra existente. Solo dueño o administrador. */
    @PutMapping("/{id}")
    public ArtworkDto updateArtwork(@PathVariable Long id, @Valid @RequestBody UpdateArtworkRequest body) {
        return artworkService.updateArtwork(id, body);
    }

    /** Habilita o deshabilita la obra para nuevas solicitudes de préstamo. */
    @PatchMapping("/{id}/availability")
    public ArtworkDto setAvailability(@PathVariable Long id, @RequestParam boolean available) {
        return artworkService.setAvailability(id, available);
    }

    /** Elimina la obra siempre que no tenga préstamos activos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtwork(@PathVariable Long id) {
        artworkService.deleteArtwork(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Documentación adjunta =====

    /**
     * Adjunta un documento a la obra (certificado, seguro, informe...).
     * Si el documento se marca como confidencial, solo el dueño y la
     * administración podrán verlo.
     */
    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    public ArtworkDto addDocument(@PathVariable Long id,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(defaultValue = "false") boolean confidential,
                                  @RequestPart("file") MultipartFile file) throws IOException {
        return artworkService.addDocument(id, description, confidential, file);
    }

    /** Elimina un documento adjunto de la obra. */
    @DeleteMapping("/{id}/documents/{artworkFileId}")
    public ArtworkDto deleteDocument(@PathVariable Long id, @PathVariable Long artworkFileId) {
        return artworkService.deleteDocument(id, artworkFileId);
    }
}
