package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.FileDto;
import com.prestarte.tfg.model.dto.UpdateArtworkRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import com.prestarte.tfg.security.CurrentUser;
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
    private final LoanRequestRepository loanRequestRepository;
    private final CurrentUser currentUser;

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

    /** Edita la obra. Solo el coleccionista dueño (o admin) puede. */
    @Transactional
    public ArtworkDto updateArtwork(Long id, UpdateArtworkRequest req) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);

        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getArtist() != null) a.setArtist(req.getArtist());
        if (req.getYear() != null) a.setYear(req.getYear());
        if (req.getWidthCm() != null) a.setWidthCm(req.getWidthCm());
        if (req.getHeightCm() != null) a.setHeightCm(req.getHeightCm());
        if (req.getDepthCm() != null) a.setDepthCm(req.getDepthCm());
        if (req.getCondition() != null) a.setCondition(Artwork.Condition.valueOf(req.getCondition()));
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        if (req.getEstimatedValue() != null) a.setEstimatedValue(req.getEstimatedValue());
        if (req.getLoanConditions() != null) a.setLoanConditions(req.getLoanConditions());
        if (req.getLocation() != null) a.setLocation(req.getLocation());

        if (req.getPreferredTransportCompanyId() != null) {
            TransportCompany tc = transportCompanyRepository.findById(req.getPreferredTransportCompanyId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Empresa de transporte", req.getPreferredTransportCompanyId()));
            a.setPreferredTransportCompany(tc);
        }
        if (req.getPreferredTransportMandatory() != null) {
            a.setPreferredTransportMandatory(req.getPreferredTransportMandatory());
        }

        return convertToDto(artworkRepository.save(a));
    }

    /** Da de baja / vuelve a poner disponible la obra para préstamos. */
    @Transactional
    public ArtworkDto setAvailability(Long id, boolean available) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);
        a.setAvailableForLoan(available);
        return convertToDto(artworkRepository.save(a));
    }

    /**
     * Elimina la obra. Solo si no tiene préstamos vivos (cualquier estado no
     * terminal); si los hay, rechazamos con 409 para preservar el histórico.
     */
    @Transactional
    public void deleteArtwork(Long id) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);

        boolean hasActive = loanRequestRepository.findByArtworkCollectorId(a.getCollector().getId())
                .stream()
                .filter(l -> l.getArtwork().getId().equals(id))
                .anyMatch(l -> {
                    LoanRequest.Status s = l.getStatus();
                    return s != LoanRequest.Status.RETURNED
                        && s != LoanRequest.Status.REJECTED
                        && s != LoanRequest.Status.CANCELLED;
                });
        if (hasActive) {
            throw new IllegalStateException(
                    "No se puede eliminar la obra porque tiene préstamos activos. " +
                    "Dala de baja primero para evitar nuevas solicitudes.");
        }
        artworkRepository.delete(a);
    }

    private void requireOwnership(Artwork a) {
        if (currentUser.isAdmin()) return;
        currentUser.requireUserId(a.getCollector().getId());
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
                .availableForLoan(artwork.isAvailableForLoan())
                .files(fileDtos)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
