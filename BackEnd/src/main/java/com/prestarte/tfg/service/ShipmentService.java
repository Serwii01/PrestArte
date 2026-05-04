package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final TransportCompanyRepository transportCompanyRepository;

    @Transactional
    public ShipmentResponse createShipment(Long loanId, Long transportCompanyId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        if (loan.getStatus() != LoanRequest.Status.ACEPTADA) {
            throw new RuntimeException("Shipment can only be created for ACCEPTED loans.");
        }

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> new RuntimeException("Transport company not found"));

        Shipment shipment = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(company)
                .trackingNumber("TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Shipment.ShipmentStatus.PENDIENTE)
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getByCompany(Long companyId) {
        return shipmentRepository.findByTransportCompanyId(companyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(Long shipmentId, Shipment.ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment record not found"));

        shipment.setStatus(newStatus);

        // If status is updated to IN_TRANSIT or similar, you could add timestamps here

        Shipment saved = shipmentRepository.save(shipment);
        return mapToResponse(saved);
    }

    @Transactional
    public ShipmentResponse confirmArrival(Long shipmentId, ConfirmReceiptRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment record not found"));

        shipment.setStatus(Shipment.ShipmentStatus.ENTREGADO);
        shipment.setReceivedBy(request.getReceivedBy());
        shipment.setNotes(request.getNotes());

        // Set the delivery date to current time
        shipment.setDeliveryDate(LocalDateTime.now());

        Shipment saved = shipmentRepository.save(shipment);

        System.out.println("Arrival confirmed for tracking: " + saved.getTrackingNumber());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Shipment getByLoanId(Long loanId) {
        return shipmentRepository.findByLoanRequestId(loanId).orElse(null);
    }

    /**
     * Internal helper to convert Entity to DTO and avoid infinite recursion
     */
    private ShipmentResponse mapToResponse(Shipment saved) {
        return ShipmentResponse.builder()
                .id(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .status(saved.getStatus().name())
                .transportCompanyName(saved.getTransportCompany().getCompanyName())
                .artworkTitle(saved.getLoanRequest().getArtwork().getTitle())
                .receivedBy(saved.getReceivedBy())
                .notes(saved.getNotes())
                .deliveryDate(saved.getDeliveryDate())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}