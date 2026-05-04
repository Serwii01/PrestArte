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

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final TransportCompanyRepository transportCompanyRepository;

    @Transactional
    public ShipmentResponse createShipment(Long loanId, Long transportCompanyId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (loan.getStatus() != LoanRequest.Status.ACEPTADA) {
            throw new RuntimeException("El préstamo debe estar ACEPTADA.");
        }

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> new RuntimeException("Empresa de transporte no encontrada"));

        Shipment shipment = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(company)
                .trackingNumber("TK-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Shipment.ShipmentStatus.PENDIENTE)
                .build();

        Shipment saved = shipmentRepository.save(shipment);

        // Mapeo a DTO para evitar el bucle infinito
        return ShipmentResponse.builder()
                .id(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .status(saved.getStatus().name())
                .transportCompanyName(company.getCompanyName())
                .artworkTitle(loan.getArtwork().getTitle())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<Shipment> getByCompany(Long companyId) {
        return shipmentRepository.findByTransportCompanyId(companyId);
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(Long shipmentId, Shipment.ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

        shipment.setStatus(newStatus);
        Shipment saved = shipmentRepository.save(shipment);

        // Devolvemos el DTO para mantener la respuesta limpia
        return ShipmentResponse.builder()
                .id(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .status(saved.getStatus().name())
                .transportCompanyName(saved.getTransportCompany().getCompanyName())
                .artworkTitle(saved.getLoanRequest().getArtwork().getTitle())
                .build();
    }

    @Transactional
    public ShipmentResponse confirmArrival(Long shipmentId, ConfirmReceiptRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

        shipment.setStatus(Shipment.ShipmentStatus.ENTREGADO);
        shipment.setReceivedBy(request.getReceivedBy());
        shipment.setNotes(request.getNotes());
        shipment.setDeliveryDate(LocalDateTime.now());

        Shipment saved = shipmentRepository.save(shipment);

        return ShipmentResponse.builder()
                .id(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .status(saved.getStatus().name())
                .artworkTitle(saved.getLoanRequest().getArtwork().getTitle())
                .receivedBy(saved.getReceivedBy())
                .notes(saved.getNotes())
                .deliveryDate(saved.getDeliveryDate())
                .transportCompanyName(saved.getTransportCompany().getCompanyName())
                .build();
    }

    public Shipment getByLoanId(Long loanId) {
        // Lógica para buscar el envío por el ID del préstamo
        return shipmentRepository.findByLoanRequestId(loanId).orElse(null);
    }
}