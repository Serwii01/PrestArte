package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.security.CurrentUser;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.prestarte.tfg.model.dto.FoundationDashboardDto;
import com.prestarte.tfg.model.dto.FoundationPendingRequestDto;
import com.prestarte.tfg.model.dto.FoundationInventoryDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio que ofrece operaciones específicas para el rol fundación.
 *
 * Compone el panel resumen, que separa las solicitudes a la espera de
 * respuesta del coleccionista y las obras que la fundación ya tiene
 * físicamente en su poder.
 */
@Service
@RequiredArgsConstructor
public class FoundationService {

    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    /**
     * Construye el resumen del panel de la fundación: peticiones
     * pendientes de respuesta y obras ya recibidas en el museo. Solo lo
     * puede consultar la propia fundación o un administrador.
     */
    @Transactional(readOnly = true)
    public FoundationDashboardDto getDashboard(Long foundationId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(foundationId);
        }
        List<LoanRequest> pending = loanRequestRepository.findByFoundationIdAndStatus(
                foundationId, LoanRequest.Status.REQUESTED);

        List<Shipment> activeShipments = shipmentRepository.findByLoanRequestFoundationIdAndStatus(
                foundationId, Shipment.ShipmentStatus.DELIVERED);

        return FoundationDashboardDto.builder()
                .pendingRequests(pending.stream()
                        .map(this::mapToPendingDto)
                        .collect(Collectors.toList()))
                .activeInventory(activeShipments.stream()
                        .map(this::mapToInventoryDto)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Compone el DTO de una solicitud pendiente, incluyendo las
     * condiciones de préstamo definidas por el coleccionista.
     */
    private FoundationPendingRequestDto mapToPendingDto(LoanRequest lr) {
        return FoundationPendingRequestDto.builder()
                .loanRequestId(lr.getId())
                .artworkTitle(lr.getArtwork().getTitle())
                .artist(lr.getArtwork().getArtist())
                .collectorName(lr.getArtwork().getCollector().getName())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .loanConditions(lr.getArtwork().getLoanConditions())
                .status(lr.getStatus().name())
                .build();
    }

    /** Compone el DTO de inventario activo a partir del envío entregado. */
    private FoundationInventoryDto mapToInventoryDto(Shipment s) {
        return FoundationInventoryDto.builder()
                .shipmentId(s.getId())
                .artworkTitle(s.getLoanRequest().getArtwork().getTitle())
                .collectorName(s.getLoanRequest().getArtwork().getCollector().getName())
                .deliveryDate(s.getDeliveryDate())
                .trackingNumber(s.getTrackingNumber())
                .transportCompany(s.getTransportCompany().getCompanyName())
                .build();
    }
}
