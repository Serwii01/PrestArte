package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CollectorDashboardDTO;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.security.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio que ofrece operaciones específicas para el rol coleccionista.
 *
 * Compone el panel resumen que agrupa sus obras, las solicitudes
 * pendientes y los envíos activos.
 */
@Service
@RequiredArgsConstructor
public class CollectorService {

    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    /**
     * Compone el panel resumen del coleccionista: obras disponibles,
     * solicitudes pendientes de respuesta y envíos en marcha. Solo lo
     * puede consultar el propio coleccionista o un administrador.
     */
    @Transactional
    public CollectorDashboardDTO getDashboard(Long collectorId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(collectorId);
        }
        List<Artwork> myArtworks = artworkRepository.findByCollectorId(collectorId);

        List<LoanResponse> pending = loanRequestRepository.findAll().stream()
                .filter(l -> l.getArtwork().getCollector().getId().equals(collectorId))
                .filter(l -> l.getStatus() == LoanRequest.Status.REQUESTED)
                .map(this::mapToLoanResponse)
                .toList();

        List<ShipmentResponse> activeShipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getLoanRequest().getArtwork().getCollector().getId().equals(collectorId))
                .map(this::mapToShipmentResponse)
                .toList();

        List<ArtworkDto> available = myArtworks.stream()
                .filter(a -> activeShipments.stream()
                        .noneMatch(s -> s.getArtworkTitle().equals(a.getTitle())))
                .map(a -> ArtworkDto.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .artist(a.getArtist())
                        .description(a.getDescription())
                        .collectorName(a.getCollector().getName())
                        .build())
                .toList();

        return CollectorDashboardDTO.builder()
                .availableArtworks(available)
                .pendingLoans(pending)
                .activeLoans(activeShipments)
                .build();
    }

    // ===== Helpers de mapeo =====

    private LoanResponse mapToLoanResponse(LoanRequest l) {
        return LoanResponse.builder()
                .id(l.getId())
                .artworkTitle(l.getArtwork().getTitle())
                .foundationName(l.getFoundation().getName())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
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
                .startDate(s.getLoanRequest().getStartDate() != null ?
                        s.getLoanRequest().getStartDate().atStartOfDay() : null)
                .endDate(s.getLoanRequest().getEndDate() != null ?
                        s.getLoanRequest().getEndDate().atStartOfDay() : null)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
