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
    private final PdfGeneratorService pdfGeneratorService; // Inyectado para la formalización

    /**
     * 1. Solicitar servicio.
     * Se invoca cuando el Museo o el Coleccionista (según mandatoryTransport) eligen empresa.
     */
    @Transactional
    public ShipmentResponse requestShipment(Long loanId, Long transportCompanyId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> new RuntimeException("Transport company not found"));

        // El préstamo pasa a fase de logística
        loan.setStatus(LoanRequest.Status.LOGISTICS_PENDING);
        loanRequestRepository.save(loan);

        Shipment shipment = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(company)
                .status(Shipment.ShipmentStatus.SOLICITADO)
                .trackingNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .priceAccepted(false)
                .insuranceValue(loan.getArtwork().getEstimatedValue()) // Valor base para el seguro
                .build();

        return mapToResponse(shipmentRepository.save(shipment));
    }
    @Transactional(readOnly = true)
    public Shipment getByLoanId(Long loanId) {
        return shipmentRepository.findByLoanRequestId(loanId)
                .orElse(null); // Devolvemos null si aún no hay transporte asignado
    }

    public Shipment getByIdRaw(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
    }
    /**
     * 2. Proponer presupuesto (Acción del Transportista).
     */
    @Transactional
    public ShipmentResponse proposeBudget(Long shipmentId, Double transportPrice, Double insuranceCost, String policy) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setPrice(transportPrice);
        shipment.setInsuranceCost(insuranceCost);
        shipment.setInsurancePolicy(policy);

        // Notificar a la fundación que ya tiene un presupuesto listo para revisar
        // emailService.sendBudgetNotification(shipment);

        return mapToResponse(shipmentRepository.save(shipment));
    }

    /**
     * 3. Aprobar presupuesto y formalizar (Acción del Museo/Fundación).
     * Cambia el préstamo a ACEPTADA y genera el contrato.
     */
    @Transactional
    public ShipmentResponse approveBudget(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getPrice() == null || shipment.getPrice() <= 0) {
            throw new RuntimeException("Cannot approve a shipment without a price proposal.");
        }

        // Bloqueamos el acuerdo
        shipment.setPriceAccepted(true);
        shipment.setStatus(Shipment.ShipmentStatus.PENDIENTE);
        shipment.setTrackingNumber(shipment.getTrackingNumber().replace("REQ-", "TK-"));

        // El préstamo ya es oficial
        LoanRequest loan = shipment.getLoanRequest();
        loan.setStatus(LoanRequest.Status.ACEPTADA);
        loanRequestRepository.save(loan);

        Shipment saved = shipmentRepository.save(shipment);

        // DISPARADOR DE FORMALIZACIÓN:
        // 1. Generar el PDF final con los datos de transporte y seguro
        // byte[] contractPdf = pdfGeneratorService.generateLoanContract(loan, saved);

        // 2. Enviar email con el PDF a las tres partes
        // emailService.sendFinalContract(loan, saved, contractPdf);

        return mapToResponse(saved);
    }

    /**
     * Actualizar estado durante el tránsito (Acción del Transportista).
     */
    @Transactional
    public ShipmentResponse updateStatus(Long shipmentId, Shipment.ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setStatus(newStatus);

        // --- NUEVA LÓGICA DE SINCRONIZACIÓN ---
        if (newStatus == Shipment.ShipmentStatus.EN_TRANSITO) {
            // En tu enum de LoanRequest falta el estado EN_TRANSITO, deberías añadirlo
            // shipment.getLoanRequest().setStatus(LoanRequest.Status.EN_TRANSITO);
        }

        return mapToResponse(shipmentRepository.save(shipment));
    }

    /**
     * Confirmar entrega (Acción del Museo).
     */
    @Transactional
    public ShipmentResponse confirmArrival(Long shipmentId, ConfirmReceiptRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        shipment.setStatus(Shipment.ShipmentStatus.ENTREGADO);
        shipment.setReceivedBy(request.getReceivedBy());
        shipment.setNotes(request.getNotes());
        shipment.setDeliveryDate(LocalDateTime.now());

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
                // --- CAMPOS AÑADIDOS PARA EL DASHBOARD ---
                .startDate(s.getLoanRequest().getStartDate() != null ?
                        s.getLoanRequest().getStartDate().atStartOfDay() : null)
                .endDate(s.getLoanRequest().getEndDate() != null ?
                        s.getLoanRequest().getEndDate().atStartOfDay() : null)
                .build();
    }   
}