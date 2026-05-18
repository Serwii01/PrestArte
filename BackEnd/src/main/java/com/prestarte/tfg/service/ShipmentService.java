package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ProposeQuoteRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
import com.prestarte.tfg.service.statemachine.LoanStateMachine;
import com.prestarte.tfg.service.statemachine.ShipmentStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica logística (envío de la obra). Cada operación valida la transición
 * mediante {@link ShipmentStateMachine} y, cuando procede, sincroniza el
 * préstamo asociado vía {@link LoanRequestService}.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentStateMachine shipmentStateMachine;
    private final CurrentUser currentUser;

    // @Lazy para romper el ciclo ShipmentService ↔ LoanRequestService
    @Lazy
    private final LoanRequestService loanRequestService;

    /* ========== CREACIÓN DE SHIPMENT (llamada desde LoanRequestService.accept) ========== */

    @Transactional
    public Shipment createOutboundShipment(LoanRequest loan, TransportCompany company) {
        Shipment shipment = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(company)
                .status(Shipment.ShipmentStatus.REQUESTED)
                .direction(Shipment.ShipmentDirection.OUTBOUND)
                .trackingNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .priceAccepted(false)
                .insuranceValue(loan.getArtwork().getEstimatedValue())
                .build();
        return shipmentRepository.save(shipment);
    }

    /**
     * Crea el Shipment de retorno al iniciar la devolución. Reutiliza la empresa
     * de transporte del OUTBOUND y arranca en APPROVED (el coste del retorno se
     * considera incluido en el presupuesto original, así que no se vuelve a pedir
     * presupuesto ni aprobación al museo).
     */
    @Transactional
    public Shipment createReturnShipment(LoanRequest loan) {
        // Cogemos el OUTBOUND más reciente: tras una reasignación puede haber
        // varios y solo el último contiene a la empresa que realmente entregó.
        Shipment outbound = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loan.getId(), Shipment.ShipmentDirection.OUTBOUND)
                .orElseThrow(() -> new IllegalStateException(
                        "No se puede crear el retorno sin un envío OUTBOUND previo."));

        Shipment ret = Shipment.builder()
                .loanRequest(loan)
                .transportCompany(outbound.getTransportCompany())
                .status(Shipment.ShipmentStatus.APPROVED)
                .direction(Shipment.ShipmentDirection.RETURN)
                .trackingNumber("RET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .priceAccepted(true)
                .insuranceValue(loan.getArtwork().getEstimatedValue())
                .build();
        return shipmentRepository.save(ret);
    }

    /* ========== ACCIONES DEL FLUJO ========== */

    /**
     * Transportista propone presupuesto. Shipment REQUESTED → QUOTED.
     * Sincroniza el préstamo a QUOTE_PROPOSED.
     */
    @Transactional
    public ShipmentResponse proposeQuote(Long shipmentId, ProposeQuoteRequest req) {
        Shipment shipment = findOrThrow(shipmentId);
        // Solo la empresa de transporte asignada puede subir el presupuesto.
        currentUser.requireUserId(shipment.getTransportCompany().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.QUOTED);

        shipment.setPrice(req.getPrice());
        shipment.setInsuranceCost(req.getInsuranceCost());
        shipment.setInsurancePolicy(req.getInsurancePolicy());
        shipment.setStatus(Shipment.ShipmentStatus.QUOTED);
        shipmentRepository.save(shipment);

        loanRequestService.onShipmentQuoted(shipment.getLoanRequest());
        return mapToResponse(shipment);
    }

    /**
     * Museo aprueba el presupuesto. Shipment QUOTED → APPROVED.
     * El avance del préstamo a PAID se hace en PaymentService (sub-fase 2.4).
     * En esta sub-fase, dejamos el préstamo en QUOTE_PROPOSED hasta que el pago se ejecute.
     *
     * NOTA: temporalmente avanzamos también el préstamo a PAID aquí para que el
     * flujo sea probable end-to-end. En 2.4 se moverá a PaymentService.
     */
    @Transactional
    public ShipmentResponse approveQuote(Long shipmentId) {
        Shipment shipment = findOrThrow(shipmentId);
        // Solo la fundación que solicitó el préstamo puede aprobar el presupuesto.
        currentUser.requireUserId(shipment.getLoanRequest().getFoundation().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.APPROVED);

        if (shipment.getPrice() == null || shipment.getPrice() <= 0) {
            throw new IllegalStateException(
                    "No se puede aprobar un envío sin una propuesta de precio.");
        }

        shipment.setPriceAccepted(true);
        shipment.setStatus(Shipment.ShipmentStatus.APPROVED);
        shipment.setTrackingNumber(shipment.getTrackingNumber().replace("REQ-", "TK-"));
        shipmentRepository.save(shipment);

        loanRequestService.onPaymentSucceeded(shipment.getLoanRequest());
        return mapToResponse(shipment);
    }

    /**
     * Museo rechaza el presupuesto. Shipment QUOTED → REJECTED.
     * El préstamo va a CANCELLED si la empresa era obligatoria, o vuelve a
     * QUOTE_PENDING para pedir presupuesto a otra empresa.
     */
    @Transactional
    public ShipmentResponse rejectQuote(Long shipmentId, String reason) {
        Shipment shipment = findOrThrow(shipmentId);
        // Rechazar presupuesto es decisión exclusiva de la fundación.
        currentUser.requireUserId(shipment.getLoanRequest().getFoundation().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.REJECTED);

        shipment.setStatus(Shipment.ShipmentStatus.REJECTED);
        shipmentRepository.save(shipment);

        loanRequestService.onQuoteRejected(shipment.getLoanRequest(), reason);
        return mapToResponse(shipment);
    }

    /**
     * Transportista marca obra recogida. APPROVED → PICKED_UP.
     * Solo el OUTBOUND mueve el préstamo a IN_TRANSIT; el shipment de RETURN
     * deja el préstamo en RETURNING hasta que se entregue al coleccionista.
     */
    @Transactional
    public ShipmentResponse markPickedUp(Long shipmentId) {
        Shipment shipment = findOrThrow(shipmentId);
        currentUser.requireUserId(shipment.getTransportCompany().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.PICKED_UP);

        shipment.setStatus(Shipment.ShipmentStatus.PICKED_UP);
        shipmentRepository.save(shipment);

        if (shipment.getDirection() == Shipment.ShipmentDirection.OUTBOUND) {
            loanRequestService.onShipmentPickedUp(shipment.getLoanRequest());
        }
        return mapToResponse(shipment);
    }

    /** Transportista marca obra en tránsito. PICKED_UP → IN_TRANSIT. */
    @Transactional
    public ShipmentResponse markInTransit(Long shipmentId) {
        Shipment shipment = findOrThrow(shipmentId);
        currentUser.requireUserId(shipment.getTransportCompany().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.IN_TRANSIT);

        shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
        shipmentRepository.save(shipment);
        // El préstamo ya está en IN_TRANSIT desde markPickedUp, no se mueve.
        return mapToResponse(shipment);
    }

    /**
     * Confirma la llegada del envío. IN_TRANSIT → DELIVERED.
     * - OUTBOUND: lo confirma el museo. Préstamo → DELIVERED → ON_LOAN.
     * - RETURN: lo confirma el coleccionista. Préstamo → RETURNED (cierre del ciclo).
     */
    @Transactional
    public ShipmentResponse confirmDelivery(Long shipmentId, ConfirmReceiptRequest request) {
        Shipment shipment = findOrThrow(shipmentId);

        boolean isReturn = shipment.getDirection() == Shipment.ShipmentDirection.RETURN;
        Long expectedUserId = isReturn
                ? shipment.getLoanRequest().getArtwork().getCollector().getId()
                : shipment.getLoanRequest().getFoundation().getId();
        currentUser.requireUserId(expectedUserId);

        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.DELIVERED);

        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        shipment.setReceivedBy(request.getReceivedBy());
        shipment.setNotes(request.getNotes());
        shipment.setDeliveryDate(LocalDateTime.now());
        shipmentRepository.save(shipment);

        if (isReturn) {
            loanRequestService.onReturnDelivered(shipment.getLoanRequest());
        } else {
            loanRequestService.onShipmentDelivered(shipment.getLoanRequest());
        }
        return mapToResponse(shipment);
    }

    /* ========== LECTURAS ========== */

    /**
     * Devuelve el Shipment OUTBOUND del préstamo (el que se usa para el
     * contrato y datos económicos). Antes existía un único shipment por loan,
     * pero ahora hay también de RETURN: filtramos por dirección para evitar
     * que Spring Data devuelva "non-unique result".
     */
    @Transactional(readOnly = true)
    public Shipment getByLoanId(Long loanId) {
        return shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loanId, Shipment.ShipmentDirection.OUTBOUND)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Shipment getByIdRaw(Long id) {
        Shipment s = findOrThrow(id);
        requireShipmentAccess(s);
        return s;
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id) {
        Shipment s = findOrThrow(id);
        requireShipmentAccess(s);
        return mapToResponse(s);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getByTransportCompany(Long companyId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(companyId);
        }
        return shipmentRepository.findByTransportCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* ========== HELPERS ========== */

    private Shipment findOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Envío", id));
    }

    /**
     * El usuario actual ha de ser parte del préstamo asociado al envío:
     * coleccionista dueño, fundación solicitante, empresa de transporte o admin.
     */
    private void requireShipmentAccess(Shipment s) {
        if (currentUser.isAdmin()) return;
        Long collectorId = s.getLoanRequest().getArtwork().getCollector().getId();
        Long foundationId = s.getLoanRequest().getFoundation().getId();
        Long transportId = s.getTransportCompany().getId();
        if (!currentUser.isAnyOf(collectorId, foundationId, transportId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes acceso a este envío");
        }
    }

    private ShipmentResponse mapToResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .loanRequestId(s.getLoanRequest() != null ? s.getLoanRequest().getId() : null)
                .transportCompanyId(s.getTransportCompany() != null ? s.getTransportCompany().getId() : null)
                .trackingNumber(s.getTrackingNumber())
                .status(s.getStatus().name())
                .direction(s.getDirection() != null ? s.getDirection().name() : null)
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
                .startDate(s.getLoanRequest().getStartDate() != null ?
                        s.getLoanRequest().getStartDate().atStartOfDay() : null)
                .endDate(s.getLoanRequest().getEndDate() != null ?
                        s.getLoanRequest().getEndDate().atStartOfDay() : null)
                .build();
    }
}
