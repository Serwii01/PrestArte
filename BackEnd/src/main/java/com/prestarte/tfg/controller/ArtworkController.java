package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.service.ArtworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks")
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;

    @PostMapping
    public ArtworkDto createArtwork(@RequestBody CreateArtworkRequest request) {
        return artworkService.createArtwork(request);
    }

    @GetMapping
    public List<ArtworkDto> getAllArtworks() {
        return artworkService.getAllArtworks();
    }

    @GetMapping("/{id}")
    public ArtworkDto getArtworkById(@PathVariable Long id) {
        return artworkService.getArtworkById(id);
    }
}