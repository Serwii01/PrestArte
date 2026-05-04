package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CollectorDashboardDTO;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final CollectorRepository collectorRepository;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Collector createCollector(Collector collector) {
        collector.setRole(Role.COLLECTOR);
        return collectorRepository.save(collector);
    }

    public List<Collector> getAllCollectors() {
        return collectorRepository.findAll();
    }

    public CollectorDashboardDTO getDashboard(Long collectorId) {
        // 1. Obtener todas las obras del coleccionista
        List<Artwork> myArtworks = artworkRepository.findByOwnerId(collectorId);

        // 2. Préstamos pendientes (donde la obra es mía y el estado es PENDIENTE)
        List<LoanResponse> pending = loanRequestRepository.findAll().stream()
                .filter(l -> l.getArtwork().getOwner().getId().equals(collectorId))
                .filter(l -> l.getStatus() == LoanRequest.Status.PENDIENTE)
                .map(this::mapToLoanResponse)
                .toList();

        // 3. Envíos activos (obras que ya tienen transporte asignado)
        List<ShipmentResponse> activeShipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getLoanRequest().getArtwork().getOwner().getId().equals(collectorId))
                .map(this::mapToShipmentResponse)
                .toList();

        // 4. Obras disponibles (aquellas que no están en la lista de envíos activos)
        List<ArtworkDto> available = myArtworks.stream()
                .filter(a -> activeShipments.stream()
                        .noneMatch(s -> s.getArtworkTitle().equals(a.getTitle())))
                .map(a -> ArtworkDto.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .artist(a.getArtist())
                        .description(a.getDescription())
                        .collectorName(a.getOwner().getName())
                        // Si necesitas el año u otros campos, añádelos aquí:
                        // .year(a.getYear())
                        .build())
                .toList();

        return CollectorDashboardDTO.builder()
                .availableArtworks(available)
                .pendingLoans(pending)
                .activeLoans(activeShipments)
                .build();
    }

    // --- MÉTODOS DE MAPEO AUXILIARES ---

    private LoanResponse mapToLoanResponse(LoanRequest l) {
        return LoanResponse.builder()
                .id(l.getId())
                .artworkTitle(l.getArtwork().getTitle())
                .foundationName(l.getFoundation().getName())
                .startDate(l.getStartDate()) // Ahora funciona
                .endDate(l.getEndDate())     // Ahora funciona
                .status(l.getStatus().name())
                .build();
    }

    private ShipmentResponse mapToShipmentResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus().name())
                .artworkTitle(s.getLoanRequest().getArtwork().getTitle())
                .transportCompanyName(s.getTransportCompany().getCompanyName())
                .receivedBy(s.getReceivedBy())
                .notes(s.getNotes())
                .deliveryDate(s.getDeliveryDate())
                // Convertimos LocalDate de la entidad a LocalDateTime del DTO
                .startDate(s.getLoanRequest().getStartDate() != null ?
                        s.getLoanRequest().getStartDate().atStartOfDay() : null)
                .endDate(s.getLoanRequest().getEndDate() != null ?
                        s.getLoanRequest().getEndDate().atStartOfDay() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}