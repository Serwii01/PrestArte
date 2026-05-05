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
    private final EmailService emailService;
    private final PdfGeneratorService pdfGeneratorService;

    @Transactional
    public ShipmentResponse requestShipment(Long loanId, Long transportCompanyId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> new RuntimeException("Transport company not found"));

        loan.setStatus(LoanRequest.Status.LOGISTICS_PENDING);
        loanRequestRepository.save(loan);

        Shipment shipment = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(company)
                .status(Shipment.ShipmentStatus.SOLICITADO)
                .trackingNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .priceAccepted(false)
                .insuranceValue(loan.getArtwork().getEstimatedValue())
                .build();

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Transactional(readOnly = true)
    public Shipment getByLoanId(Long loanId) {
        return shipmentRepository.findByLoanRequestId(loanId).orElse(null);
    }

    public Shipment getByIdRaw(Long id) {
        return shipmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Shipment not found"));
    }

    @Transactional
    public ShipmentResponse proposeBudget(Long shipmentId, Double transportPrice, Double insuranceCost, String policy) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setPrice(transportPrice);
        shipment.setInsuranceCost(insuranceCost);
        shipment.setInsurancePolicy(policy);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse approveBudget(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getPrice() == null || shipment.getPrice() <= 0) {
            throw new RuntimeException("Cannot approve a shipment without a price proposal.");
        }

        shipment.setPriceAccepted(true);
        shipment.setStatus(Shipment.ShipmentStatus.PENDIENTE);
        shipment.setTrackingNumber(shipment.getTrackingNumber().replace("REQ-", "TK-"));

        LoanRequest loan = shipment.getLoanRequest();
        loan.setStatus(LoanRequest.Status.ACEPTADA);
        loanRequestRepository.save(loan);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    /**
     * Actualizar estado durante el tránsito (Acción del Transportista).
     * SINCRONIZADO: Cambia el estado del préstamo a EN_TRANSITO.
     */
    @Transactional
    public ShipmentResponse updateStatus(Long shipmentId, Shipment.ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setStatus(newStatus);

        // Sincronización con el estado del préstamo[cite: 4, 7]
        if (newStatus == Shipment.ShipmentStatus.RECOGIDO || newStatus == Shipment.ShipmentStatus.EN_TRANSITO) {
            shipment.getLoanRequest().setStatus(LoanRequest.Status.EN_TRANSITO);
            loanRequestRepository.save(shipment.getLoanRequest());
        }

        return mapToResponse(shipmentRepository.save(shipment));
    }

    /**
     * Confirmar entrega (Acción del Museo).
     * SINCRONIZADO: Cambia el estado del préstamo a RECIBIDA[cite: 4, 7].
     */
    @Transactional
    public ShipmentResponse confirmArrival(Long shipmentId, ConfirmReceiptRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setStatus(Shipment.ShipmentStatus.ENTREGADO);
        shipment.setReceivedBy(request.getReceivedBy());
        shipment.setNotes(request.getNotes());
        shipment.setDeliveryDate(LocalDateTime.now());

        // Actualizamos el préstamo a RECIBIDA[cite: 4, 7]
        shipment.getLoanRequest().setStatus(LoanRequest.Status.RECIBIDA);
        loanRequestRepository.save(shipment.getLoanRequest());

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getByTransportCompany(Long companyId) {
        return shipmentRepository.findByTransportCompanyId(companyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ShipmentResponse mapToResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus().name())
                .transportCompanyName(s.getTransportCompany().getCompanyName())
                .artworkTitle(s.getLoanRequest().getArtwork().getTitle())
                .price(s.getPrice())
                .insuranceCost(s.getInsuranceCost())
                .insuranceValue(s.getInsuranceValue())
                .insurancePolicy(s.getInsurancePolicy())
                .priceAccepted(s.isPriceAccepted())
                .receivedBy(s.getReceivedBy())
                .notes(s.getNotes())
                .deliveryDate(s.getDeliveryDate())
                .createdAt(s.getCreatedAt())
                // Asignación de fechas del préstamo para los Dashboards
                .startDate(s.getLoanRequest().getStartDate() != null ?
                        s.getLoanRequest().getStartDate().atStartOfDay() : null)
                .endDate(s.getLoanRequest().getEndDate() != null ?
                        s.getLoanRequest().getEndDate().atStartOfDay() : null)
                .build();
    }
}