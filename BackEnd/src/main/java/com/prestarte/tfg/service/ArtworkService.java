package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.FileDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final CollectorRepository collectorRepository;

    @Transactional
    public ArtworkDto createArtwork(CreateArtworkRequest request) {
        // Validamos que el ID pertenezca a un Coleccionista.
        // Si se pasa el ID de un Museo, el repositorio de Coleccionistas no lo encontrará.
        Collector collector = collectorRepository.findById(request.getCollectorId())
                .orElseThrow(() -> new RuntimeException("Error: La obra debe pertenecer a un Coleccionista válido. ID no encontrado: " + request.getCollectorId()));

        Artwork artwork = new Artwork();
        artwork.setTitle(request.getTitle());
        artwork.setArtist(request.getArtist());
        artwork.setYear(request.getYear());
        artwork.setWidthCm(request.getWidthCm());
        artwork.setHeightCm(request.getHeightCm());
        artwork.setDepthCm(request.getDepthCm());
        artwork.setDescription(request.getDescription());

        // Establecemos la relación con el coleccionista
        artwork.setCollector(collector);

        // Gestión del estado de conservación
        if (request.getCondition() != null) {
            try {
                artwork.setCondition(Artwork.Condition.valueOf(request.getCondition().toUpperCase()));
            } catch (IllegalArgumentException e) {
                artwork.setCondition(null);
            }
        }

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
        List<FileDto> fileDtos = new ArrayList<>();
        if (artwork.getFiles() != null) {
            fileDtos = artwork.getFiles().stream()
                    .map(af -> FileDto.builder()
                            .id(af.getFile().getId())
                            .fileName(af.getFile().getFileName())
                            .fileType(af.getFile().getFileType())
                            .build())
                    .collect(Collectors.toList());
        }

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
                .files(fileDtos)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}