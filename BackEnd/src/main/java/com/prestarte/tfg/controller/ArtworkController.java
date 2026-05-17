package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.UpdateArtworkRequest;
import com.prestarte.tfg.service.ArtworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;

    @PostMapping
    public ResponseEntity<ArtworkDto> createArtwork(@Valid @RequestBody CreateArtworkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artworkService.createArtwork(request));
    }

    @GetMapping
    public List<ArtworkDto> getAllArtworks() {
        return artworkService.getAllArtworks();
    }

    @GetMapping("/{id}")
    public ArtworkDto getArtworkById(@PathVariable Long id) {
        return artworkService.getArtworkById(id);
    }

    @GetMapping("/collector/{collectorId}")
    public List<ArtworkDto> getArtworksByCollector(@PathVariable Long collectorId) {
        return artworkService.getArtworksByCollector(collectorId);
    }

    /** Editar obra (solo dueño o admin). */
    @PutMapping("/{id}")
    public ArtworkDto updateArtwork(@PathVariable Long id, @Valid @RequestBody UpdateArtworkRequest body) {
        return artworkService.updateArtwork(id, body);
    }

    /** Dar de baja / volver a publicar para préstamo. */
    @PatchMapping("/{id}/availability")
    public ArtworkDto setAvailability(@PathVariable Long id, @RequestParam boolean available) {
        return artworkService.setAvailability(id, available);
    }

    /** Eliminar obra (rechazado si tiene préstamos activos). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtwork(@PathVariable Long id) {
        artworkService.deleteArtwork(id);
        return ResponseEntity.noContent().build();
    }
}
