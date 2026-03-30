package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.entity.Artwork;
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
    public Artwork createArtwork(@RequestBody Artwork artwork) {
        return artworkService.createArtwork(artwork);
    }

    @GetMapping
    public List<Artwork> getAllArtworks() {
        return artworkService.getAllArtworks();
    }

    @GetMapping("/{id}")
    public Artwork getArtworkById(@PathVariable Long id) {
        return artworkService.getArtworkById(id)
                .orElseThrow(() -> new RuntimeException("Artwork not found"));
    }
}