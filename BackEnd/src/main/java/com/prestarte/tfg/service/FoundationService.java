package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.*;
import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.repository.FoundationRepository;
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

@Service
@RequiredArgsConstructor
public class FoundationService {

    private final FoundationRepository foundationRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * Crea una nueva fundación y le asigna el rol correspondiente.
     */
    @Transactional
    public Foundation createFoundation(Foundation foundation) {
        foundation.setRole(Role.FOUNDATION);
        return foundationRepository.save(foundation);
    }

    /**
     * Lista todas las fundaciones registradas.
     */
    public List<Foundation> getAllFoundations() {
        return foundationRepository.findAll();
    }

    /**
     * Genera la información para el Dashboard de la Fundación.
     * Separa las peticiones que están esperando respuesta de las obras que ya están en el museo.
     */
    @Transactional(readOnly = true)
    public FoundationDashboardDto getDashboard(Long foundationId) {
        // 1. Peticiones enviadas por esta fundación que siguen pendientes
        List<LoanRequest> pending = loanRequestRepository.findByFoundationIdAndStatus(
                foundationId, LoanRequest.Status.REQUESTED);

        // 2. Obras que la fundación ya tiene físicamente (envío entregado)
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
     * Mapea una LoanRequest a un DTO de petición pendiente para el museo.
     * Incluye las condiciones del préstamo que el coleccionista definió.
     */
    private FoundationPendingRequestDto mapToPendingDto(LoanRequest lr) {
        return FoundationPendingRequestDto.builder()
                .loanRequestId(lr.getId())
                .artworkTitle(lr.getArtwork().getTitle())
                .artist(lr.getArtwork().getArtist())
                .collectorName(lr.getArtwork().getCollector().getName())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                // Aquí extraemos las condiciones que irán al PDF
                .loanConditions(lr.getArtwork().getLoanConditions())
                .status(lr.getStatus().name())
                .build();
    }

    /**
     * Mapea un Shipment a un DTO de inventario activo para el museo.
     */
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