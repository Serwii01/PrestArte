package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ConfirmReceiptRequest;
import com.prestarte.tfg.model.dto.ProposeQuoteRequest;
import com.prestarte.tfg.model.dto.ShipmentResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
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
 * Servicio que gestiona la operativa logística de los envíos.
 *
 * Cubre tanto la creación de los envíos (ida y vuelta) como las
 * transiciones de su máquina de estados: subida de presupuesto,
 * aprobación o rechazo por parte del museo, recogida, tránsito y
 * entrega. Cada acción notifica al servicio de préstamos para que el
 * estado del préstamo asociado avance de forma coordinada.
 */
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentStateMachine shipmentStateMachine;
    private final CurrentUser currentUser;

    // Se inyecta con @Lazy porque existe una dependencia cíclica entre
    // ShipmentService y LoanRequestService.
    @Lazy
    private final LoanRequestService loanRequestService;

    // ===== Creación de envíos =====

    /**
     * Crea el envío de ida asociado a un préstamo recién aceptado.
     * Arranca en estado REQUESTED a la espera de que la empresa de
     * transporte suba un presupuesto.
     */
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
     * Crea el envío de devolución cuando el museo inicia el retorno.
     * Reutiliza la empresa de transporte del envío de ida más reciente
     * y arranca en estado APPROVED, ya que el coste del retorno se
     * considera incluido en el presupuesto original.
     */
    @Transactional
    public Shipment createReturnShipment(LoanRequest loan) {
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

    // ===== Acciones del flujo =====

    /**
     * La empresa de transporte sube su presupuesto. Avanza el envío
     * de REQUESTED a QUOTED y sincroniza el préstamo, que pasa a
     * QUOTE_PROPOSED a la espera de la aprobación del museo.
     */
    @Transactional
    public ShipmentResponse proposeQuote(Long shipmentId, ProposeQuoteRequest req) {
        Shipment shipment = findOrThrow(shipmentId);
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
     * El museo aprueba el presupuesto. Avanza el envío a APPROVED y
     * notifica al préstamo, que pasa a PAID. Se exige que el envío
     * tenga un precio propuesto antes de poder aprobarlo.
     */
    @Transactional
    public ShipmentResponse approveQuote(Long shipmentId) {
        Shipment shipment = findOrThrow(shipmentId);
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
     * El museo rechaza el presupuesto. Marca el envío como REJECTED y
     * delega en el servicio de préstamos la decisión sobre el
     * expediente: cancelación si la empresa era obligatoria, vuelta a
     * QUOTE_PENDING en caso contrario.
     */
    @Transactional
    public ShipmentResponse rejectQuote(Long shipmentId, String reason) {
        Shipment shipment = findOrThrow(shipmentId);
        currentUser.requireUserId(shipment.getLoanRequest().getFoundation().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.REJECTED);

        shipment.setStatus(Shipment.ShipmentStatus.REJECTED);
        shipmentRepository.save(shipment);

        loanRequestService.onQuoteRejected(shipment.getLoanRequest(), reason);
        return mapToResponse(shipment);
    }

    /**
     * La empresa de transporte indica que ha recogido la obra. Solo el
     * envío de ida desencadena la transición del préstamo a
     * IN_TRANSIT; el envío de retorno mantiene al préstamo en
     * RETURNING hasta que se confirme la entrega final.
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

    /** La empresa de transporte marca el envío como en tránsito. */
    @Transactional
    public ShipmentResponse markInTransit(Long shipmentId) {
        Shipment shipment = findOrThrow(shipmentId);
        currentUser.requireUserId(shipment.getTransportCompany().getId());
        shipmentStateMachine.validate(shipment.getStatus(), Shipment.ShipmentStatus.IN_TRANSIT);

        shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
        shipmentRepository.save(shipment);
        return mapToResponse(shipment);
    }

    /**
     * Confirma la entrega de un envío. Si es el de ida, lo confirma el
     * museo y el préstamo avanza a DELIVERED y luego a ON_LOAN. Si es
     * el de retorno, lo confirma el coleccionista y el préstamo se
     * cierra como RETURNED.
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

    // ===== Lecturas =====

    /**
     * Devuelve el envío de ida activo de un préstamo. Se elige el más
     * reciente porque un préstamo puede haber pasado por varios envíos
     * OUTBOUND como consecuencia de una reasignación de empresa.
     */
    @Transactional(readOnly = true)
    public Shipment getByLoanId(Long loanId) {
        return shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loanId, Shipment.ShipmentDirection.OUTBOUND)
                .orElse(null);
    }

    /** Devuelve la entidad del envío comprobando los permisos del usuario actual. */
    @Transactional(readOnly = true)
    public Shipment getByIdRaw(Long id) {
        Shipment s = findOrThrow(id);
        requireShipmentAccess(s);
        return s;
    }

    /** Devuelve el DTO del envío comprobando los permisos del usuario actual. */
    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id) {
        Shipment s = findOrThrow(id);
        requireShipmentAccess(s);
        return mapToResponse(s);
    }

    /** Devuelve los envíos de una empresa de transporte concreta. */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getByTransportCompany(Long companyId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(companyId);
        }
        return shipmentRepository.findByTransportCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ===== Helpers privados =====

    private Shipment findOrThrow(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Envío", id));
    }

    /**
     * Verifica que el usuario actual está autorizado a ver el envío.
     * Solo tienen acceso el coleccionista dueño, la fundación
     * solicitante, la empresa de transporte responsable del envío y
     * los administradores.
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

    /** Compone el DTO de respuesta a partir de la entidad de envío. */
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
