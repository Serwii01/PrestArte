package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.FileDto;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import com.prestarte.tfg.repository.TransportCompanyRepository;
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
    private final TransportCompanyRepository transportCompanyRepository;

    @Transactional
    public ArtworkDto createArtwork(CreateArtworkRequest request) {
        Collector collector = collectorRepository.findById(request.getCollectorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Coleccionista", request.getCollectorId()));

        // Empresa de transporte preferida (opcional)
        TransportCompany preferred = null;
        if (request.getPreferredTransportCompanyId() != null) {
            preferred = transportCompanyRepository.findById(request.getPreferredTransportCompanyId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Empresa de transporte", request.getPreferredTransportCompanyId()));
        }

        Artwork artwork = Artwork.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .year(request.getYear())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .depthCm(request.getDepthCm())
                .description(request.getDescription())
                .estimatedValue(request.getEstimatedValue())
                .loanConditions(request.getLoanConditions())
                .location(request.getLocation())
                .collector(collector)
                .preferredTransportCompany(preferred)
                .preferredTransportMandatory(
                        preferred != null && request.isPreferredTransportMandatory())
                .build();

        // El @Pattern del DTO ya valida el enum; aquí parseamos sin try/catch.
        artwork.setCondition(Artwork.Condition.valueOf(request.getCondition()));

        Artwork saved = artworkRepository.save(artwork);
        return convertToDto(saved);
    }

    public List<ArtworkDto> getAllArtworks() {
        return artworkRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ArtworkDto getArtworkById(Long id) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        return convertToDto(artwork);
    }

    public List<ArtworkDto> getArtworksByCollector(Long collectorId) {
        return artworkRepository.findByCollectorId(collectorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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

        TransportCompany preferred = artwork.getPreferredTransportCompany();

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
                .estimatedValue(artwork.getEstimatedValue())
                .loanConditions(artwork.getLoanConditions())
                .location(artwork.getLocation())
                .collectorId(artwork.getCollector() != null ? artwork.getCollector().getId() : null)
                .collectorName(artwork.getCollector() != null ? artwork.getCollector().getName() : "Anónim@")
                .preferredTransportCompanyId(preferred != null ? preferred.getId() : null)
                .preferredTransportCompanyName(preferred != null ? preferred.getCompanyName() : null)
                .preferredTransportMandatory(artwork.isPreferredTransportMandatory())
                .files(fileDtos)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
