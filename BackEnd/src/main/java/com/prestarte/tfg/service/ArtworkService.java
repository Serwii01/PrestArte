package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CollectorRepository collectorRepository;

    @Transactional
    public ArtworkDto createArtwork(CreateArtworkRequest request) {
        // 1. Buscar al coleccionista que será dueño de la obra
        Collector collector = collectorRepository.findById(request.getCollectorId())
                .orElseThrow(() -> new RuntimeException("Coleccionista no encontrado con ID: " + request.getCollectorId()));

        // 2. Mapear el Request DTO a la Entidad Artwork
        Artwork artwork = new Artwork();
        artwork.setTitle(request.getTitle());
        artwork.setArtist(request.getArtist());
        artwork.setYear(request.getYear());
        artwork.setWidthCm(request.getWidthCm());
        artwork.setHeightCm(request.getHeightCm());
        artwork.setDepthCm(request.getDepthCm());
        artwork.setDescription(request.getDescription());
        artwork.setCollector(collector);

        // CORRECCIÓN AQUÍ: Usamos Artwork.Condition en lugar de EstadoConservacion
        if (request.getCondition() != null) {
            try {
                artwork.setCondition(Artwork.Condition.valueOf(request.getCondition().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Si el valor enviado no coincide con (EXCELENTE, BUENO, REGULAR, MALO, RESTAURACION)
                artwork.setCondition(null);
            }
        }

        // 3. Guardar y devolver como DTO
        Artwork savedArtwork = artworkRepository.save(artwork);
        return convertToDto(savedArtwork);
    }

    public List<ArtworkDto> getAllArtworks() {
        return artworkRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ArtworkDto getArtworkById(Long id) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Obra no encontrada con ID: " + id));
        return convertToDto(artwork);
    }

    private ArtworkDto convertToDto(Artwork artwork) {
        return ArtworkDto.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .artist(artwork.getArtist())
                .year(artwork.getYear())
                .widthCm(artwork.getWidthCm())
                .heightCm(artwork.getHeightCm())
                .depthCm(artwork.getDepthCm())
                .condition(artwork.getCondition() != null ? artwork.getCondition().name() : null)
                .description(artwork.getDescription())
                .loanConditions(artwork.getLoanConditions())
                .collectorName(artwork.getCollector() != null ? artwork.getCollector().getName() : "Anónimo")
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}