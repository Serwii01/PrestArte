package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
import com.prestarte.tfg.service.statemachine.LoanStateMachine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private static final Logger log = LoggerFactory.getLogger(LoanRequestService.class);

    private final LoanRequestRepository loanRequestRepository;
    private final ArtworkRepository artworkRepository;
    private final FoundationRepository foundationRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final ShipmentRepository shipmentRepository;

    private final PdfGeneratorService pdfGeneratorService;
    private final EmailService emailService;
    private final ShipmentService shipmentService;
    private final LoanStateMachine stateMachine;
    private final CurrentUser currentUser;

    /* ========== CREACIÓN ========== */

    @Transactional
    public LoanResponse createRequest(CreateLoanRequest dto) {
        // Solo la propia fundación puede crear solicitudes a su nombre.
        currentUser.requireUserId(dto.getFoundationId());

        Artwork artwork = artworkRepository.findById(dto.getArtworkId())
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", dto.getArtworkId()));

        if (!artwork.isAvailableForLoan()) {
            throw new IllegalStateException(
                    "Esta obra no está disponible para préstamo en este momento.");
        }

        Foundation foundation = foundationRepository.findById(dto.getFoundationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Fundación", dto.getFoundationId()));

        LoanRequest request = LoanRequest.builder()
                .artwork(artwork)
                .foundation(foundation)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .agreedConditions(dto.getAgreedConditions())
                .status(LoanRequest.Status.REQUESTED)
                .transportCompanyMandatory(false)
                .build();

        return convertToResponse(loanRequestRepository.save(request));
    }

    /* ========== ACCIONES DEL FLUJO ========== */

    /**
     * Coleccionista acepta la solicitud y elige empresa de transporte.
     * Pasa el estado a ACCEPTED y crea inmediatamente el Shipment OUTBOUND
     * en estado REQUESTED, lo que también deja al préstamo en QUOTE_PENDING.
     */
    @Transactional
    public LoanResponse accept(Long loanId, Long transportCompanyId, boolean mandatory) {
        LoanRequest loan = findOrThrow(loanId);
        // Solo el coleccionista dueño de la obra puede aceptar.
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());

        // Validación de fechas (regla de negocio: no solapar préstamos aceptados de la misma obra)
        boolean overlapping = loanRequestRepository.existsOverlappingLoan(
                loan.getArtwork().getId(), loan.getStartDate(), loan.getEndDate());
        if (overlapping) {
            throw new IllegalStateException(
                    "Esta obra ya está reservada para las fechas seleccionadas.");
        }

        // Verificar que la empresa de transporte existe
        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", transportCompanyId));

        // Transición REQUESTED → ACCEPTED
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.ACCEPTED);
        loan.setStatus(LoanRequest.Status.ACCEPTED);
        loan.setTransportCompanyMandatory(mandatory);
        loanRequestRepository.save(loan);

        // Crea el shipment OUTBOUND en REQUESTED y avanza el préstamo a QUOTE_PENDING
        shipmentService.createOutboundShipment(loan, company);
        stateMachine.validate(LoanRequest.Status.ACCEPTED, LoanRequest.Status.QUOTE_PENDING);
        loan.setStatus(LoanRequest.Status.QUOTE_PENDING);

        return convertToResponse(loanRequestRepository.save(loan));
    }

    /** Coleccionista rechaza la solicitud (terminal). */
    @Transactional
    public LoanResponse reject(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.REJECTED);
        loan.setStatus(LoanRequest.Status.REJECTED);
        return convertToResponse(loanRequestRepository.save(loan));
    }

    /**
     * Reasignación de empresa de transporte tras el rechazo de un presupuesto.
     *
     * Cuando el museo rechaza el presupuesto y la empresa NO era obligatoria,
     * el préstamo vuelve a QUOTE_PENDING pero el viejo Shipment OUTBOUND queda
     * en REJECTED. Este método crea un Shipment OUTBOUND nuevo con la empresa
     * elegida; el préstamo se mantiene en QUOTE_PENDING esperando el nuevo
     * presupuesto.
     *
     * Permitido a la fundación solicitante o al coleccionista dueño. Si la
     * empresa era obligatoria, no hay reasignación posible (el préstamo se
     * habría cancelado automáticamente).
     */
    @Transactional
    public LoanResponse reassignTransport(Long loanId, Long newTransportCompanyId) {
        LoanRequest loan = findOrThrow(loanId);
        // Solo las dos partes contratantes pueden reasignar.
        currentUser.requireAnyUserId(
                loan.getArtwork().getCollector().getId(),
                loan.getFoundation().getId());

        if (loan.getStatus() != LoanRequest.Status.QUOTE_PENDING) {
            throw new IllegalStateException(
                    "Solo se puede asignar otra empresa de transporte cuando el préstamo " +
                    "está esperando presupuesto (QUOTE_PENDING).");
        }
        if (loan.isTransportCompanyMandatory()) {
            throw new IllegalStateException(
                    "La empresa de transporte era obligatoria para este préstamo: " +
                    "no se puede reasignar.");
        }

        // Comprobamos que el OUTBOUND más reciente está realmente rechazado.
        // Si está en REQUESTED o QUOTED, hay una negociación viva: no permitimos
        // reasignar sin antes rechazarla expresamente.
        Shipment lastOutbound = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loanId, Shipment.ShipmentDirection.OUTBOUND)
                .orElseThrow(() -> new IllegalStateException(
                        "Este préstamo no tiene un envío previo que reasignar."));
        if (lastOutbound.getStatus() != Shipment.ShipmentStatus.REJECTED) {
            throw new IllegalStateException(
                    "Ya hay un presupuesto vivo con la empresa actual. " +
                    "Recházalo antes de buscar otra empresa.");
        }

        TransportCompany company = transportCompanyRepository.findById(newTransportCompanyId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Empresa de transporte", newTransportCompanyId));

        // No tiene sentido asignar la misma empresa que acaba de ser rechazada.
        if (lastOutbound.getTransportCompany().getId().equals(newTransportCompanyId)) {
            throw new IllegalStateException(
                    "Selecciona una empresa distinta a la que acaba de rechazar el presupuesto.");
        }

        // Crea un nuevo OUTBOUND en REQUESTED. El préstamo se queda en
        // QUOTE_PENDING (no hay cambio de estado del loan).
        shipmentService.createOutboundShipment(loan, company);

        return convertToResponse(loan);
    }

    /** Coleccionista o museo cancelan el préstamo antes de que la obra esté en tránsito. */
    @Transactional
    public LoanResponse cancel(Long loanId, String reason) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireAnyUserId(
                loan.getArtwork().getCollector().getId(),
                loan.getFoundation().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.CANCELLED);
        loan.setStatus(LoanRequest.Status.CANCELLED);
        loan.setCancelledAt(LocalDateTime.now());
        loan.setCancellationReason(reason);
        return convertToResponse(loanRequestRepository.save(loan));
    }

    /** Coleccionista marca la obra como lista para recoger (tras pago aprobado). */
    @Transactional
    public LoanResponse markReadyForPickup(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.READY_FOR_PICKUP);
        loan.setStatus(LoanRequest.Status.READY_FOR_PICKUP);
        return convertToResponse(loanRequestRepository.save(loan));
    }

    /**
     * Museo inicia el retorno: el préstamo pasa a RETURNING y se genera
     * automáticamente un Shipment de RETURN con la misma empresa de transporte
     * que el OUTBOUND, en estado APPROVED (el coste del retorno se considera
     * incluido en el presupuesto original).
     */
    @Transactional
    public LoanResponse startReturn(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getFoundation().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.RETURNING);
        loan.setStatus(LoanRequest.Status.RETURNING);
        LoanRequest saved = loanRequestRepository.save(loan);

        // Crear el Shipment de retorno asociado.
        shipmentService.createReturnShipment(saved);

        return convertToResponse(saved);
    }

    /**
     * Cierre del ciclo cuando el Shipment de RETURN llega a DELIVERED.
     * Llamado por ShipmentService.confirmDelivery cuando el shipment es de retorno.
     */
    @Transactional
    public void onReturnDelivered(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.RETURNED);
        loan.setStatus(LoanRequest.Status.RETURNED);
        loanRequestRepository.save(loan);
    }

    /* ========== ACCIONES DESDE ShipmentService (sincronización) ========== */

    /**
     * Llamado por ShipmentService cuando el transportista propone presupuesto.
     * Transición QUOTE_PENDING → QUOTE_PROPOSED.
     */
    @Transactional
    public void onShipmentQuoted(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.QUOTE_PROPOSED);
        loan.setStatus(LoanRequest.Status.QUOTE_PROPOSED);
        loanRequestRepository.save(loan);
    }

    /**
     * Llamado por PaymentService (sub-fase 2.4) cuando el museo aprueba y paga.
     * Transición QUOTE_PROPOSED → PAID.
     */
    @Transactional
    public void onPaymentSucceeded(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.PAID);
        loan.setStatus(LoanRequest.Status.PAID);
        loanRequestRepository.save(loan);
        notifyPartiesOnAcceptance(loan);
    }

    /**
     * Llamado cuando el museo rechaza el presupuesto.
     * Si la empresa era obligatoria → CANCELLED. Si no → vuelve a QUOTE_PENDING
     * para pedir presupuesto a otra empresa.
     */
    @Transactional
    public void onQuoteRejected(LoanRequest loan, String reason) {
        if (loan.isTransportCompanyMandatory()) {
            stateMachine.validate(loan.getStatus(), LoanRequest.Status.CANCELLED);
            loan.setStatus(LoanRequest.Status.CANCELLED);
            loan.setCancelledAt(LocalDateTime.now());
            loan.setCancellationReason(reason != null ? reason
                    : "Presupuesto rechazado y la empresa de transporte era obligatoria.");
        } else {
            stateMachine.validate(loan.getStatus(), LoanRequest.Status.QUOTE_PENDING);
            loan.setStatus(LoanRequest.Status.QUOTE_PENDING);
        }
        loanRequestRepository.save(loan);
    }

    /** Llamado por ShipmentService cuando el transportista marca recogida. */
    @Transactional
    public void onShipmentPickedUp(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.IN_TRANSIT);
        loan.setStatus(LoanRequest.Status.IN_TRANSIT);
        loanRequestRepository.save(loan);
    }

    /** Llamado por ShipmentService cuando el museo confirma llegada. */
    @Transactional
    public void onShipmentDelivered(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.DELIVERED);
        loan.setStatus(LoanRequest.Status.DELIVERED);
        loanRequestRepository.save(loan);
        // Avance automático a ON_LOAN: el préstamo está activo en cuanto llega
        stateMachine.validate(LoanRequest.Status.DELIVERED, LoanRequest.Status.ON_LOAN);
        loan.setStatus(LoanRequest.Status.ON_LOAN);
        loanRequestRepository.save(loan);
    }

    /* ========== LECTURAS ========== */

    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        LoanRequest loan = findOrThrow(id);
        requireLoanAccess(loan);
        return convertToResponse(loan);
    }

    /**
     * Devuelve la entidad cruda (no DTO). Uso interno para generación de PDF
     * o flujos que necesitan acceso a relaciones lazy dentro de su transacción.
     */
    @Transactional(readOnly = true)
    public LoanRequest getEntityById(Long id) {
        LoanRequest loan = findOrThrow(id);
        requireLoanAccess(loan);
        // Forzamos la carga de las relaciones que el PDF necesita,
        // dentro de la transacción.
        loan.getArtwork().getCollector().getName();
        loan.getFoundation().getName();
        return loan;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getRequestsByFoundation(Long foundationId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(foundationId);
        }
        return loanRequestRepository.findByFoundationId(foundationId).stream()
                .map(this::convertToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getRequestsByCollector(Long collectorId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(collectorId);
        }
        return loanRequestRepository.findByArtworkCollectorId(collectorId).stream()
                .map(this::convertToResponse).toList();
    }

    /** Solo admin puede listar todos los préstamos. */
    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoanRequests() {
        if (!currentUser.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo los administradores pueden listar todos los préstamos");
        }
        return loanRequestRepository.findAll().stream()
                .map(this::convertToResponse).toList();
    }

    /* ========== HELPERS ========== */

    private LoanRequest findOrThrow(Long id) {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", id));
    }

    /**
     * Comprueba que el usuario actual puede VER este préstamo: ha de ser el
     * coleccionista dueño, la fundación solicitante, la empresa de transporte
     * de alguno de los shipments asociados, o un administrador.
     */
    private void requireLoanAccess(LoanRequest loan) {
        if (currentUser.isAdmin()) return;
        Long collectorId = loan.getArtwork().getCollector().getId();
        Long foundationId = loan.getFoundation().getId();
        if (currentUser.isAnyOf(collectorId, foundationId)) return;
        // Cualquier empresa de transporte que haya participado en el préstamo
        // (incluida una que fue rechazada antes de reasignar) puede consultarlo.
        // Recorremos sus propios envíos buscando uno asociado a este loan.
        boolean isTransport = shipmentRepository.findByTransportCompanyId(currentUser.currentId())
                .stream()
                .anyMatch(s -> loan.getId().equals(s.getLoanRequest().getId()));
        if (!isTransport) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes acceso a este préstamo");
        }
    }

    /**
     * Notifica a las tres partes (fundación, coleccionista y transportista)
     * con el contrato formal en PDF al confirmarse el pago.
     */
    private void notifyPartiesOnAcceptance(LoanRequest loan) {
        try {
            Shipment shipment = shipmentService.getByLoanId(loan.getId());
            byte[] pdfBytes = pdfGeneratorService.generateLoanContract(loan, shipment);

            String fileName = "Contrato_" + loan.getArtwork().getTitle().replace(" ", "_") + ".pdf";
            String subject = "CONTRATO DE PRÉSTAMO FORMALIZADO: " + loan.getArtwork().getTitle();

            String foundationEmail = loan.getFoundation().getEmail();
            if (foundationEmail != null) {
                String body = "<h3>Contrato de préstamo - Copia para la parte receptora</h3>" +
                        "<p>Hola, equipo de <b>" + loan.getFoundation().getName() + "</b>,</p>" +
                        "<p>El presupuesto ha sido aprobado y el contrato está formalizado. " +
                        "Adjuntamos el documento legal correspondiente.</p>";
                emailService.sendEmailWithAttachment(foundationEmail, subject, body, pdfBytes, fileName);
            }

            String collectorEmail = loan.getArtwork().getCollector().getEmail();
            if (collectorEmail != null) {
                String body = "<h3>Contrato de préstamo - Copia para la propiedad</h3>" +
                        "<p>Hola <b>" + loan.getArtwork().getCollector().getName() + "</b>,</p>" +
                        "<p>El museo ha aprobado el presupuesto del préstamo de <i>" +
                        loan.getArtwork().getTitle() + "</i>. Adjuntamos tu copia del acuerdo firmado.</p>";
                emailService.sendEmailWithAttachment(collectorEmail, subject, body, pdfBytes, fileName);
            }

            if (shipment != null && shipment.getTransportCompany() != null) {
                String transportEmail = shipment.getTransportCompany().getEmail();
                if (transportEmail != null) {
                    String body = "<h3>Orden de transporte de obra de arte</h3>" +
                            "<p>Tu presupuesto para el transporte de <b>" +
                            loan.getArtwork().getTitle() + "</b> ha sido aprobado.</p>" +
                            "<p>Adjuntamos el contrato con todos los requisitos de seguridad y manipulación.</p>";
                    emailService.sendEmailWithAttachment(transportEmail,
                            "NUEVO SERVICIO: " + loan.getArtwork().getTitle(),
                            body, pdfBytes, fileName);
                }
            }

            log.info("Notificaciones enviadas a las tres partes del préstamo {}.", loan.getId());

        } catch (Exception e) {
            log.error("Error enviando notificaciones del préstamo {}: {}", loan.getId(), e.getMessage(), e);
        }
    }

    private LoanResponse convertToResponse(LoanRequest l) {
        // Siempre usamos el OUTBOUND más reciente: si hubo reasignación, el que
        // está vivo es el último; los anteriores están en REJECTED.
        Shipment outbound = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        l.getId(), Shipment.ShipmentDirection.OUTBOUND)
                .orElse(null);
        Shipment returnShip = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        l.getId(), Shipment.ShipmentDirection.RETURN)
                .orElse(null);

        return LoanResponse.builder()
                .id(l.getId())
                .artworkId(l.getArtwork().getId())
                .artworkTitle(l.getArtwork().getTitle())
                .artworkArtist(l.getArtwork().getArtist())
                .collectorId(l.getArtwork().getCollector().getId())
                .collectorName(l.getArtwork().getCollector().getName())
                .foundationId(l.getFoundation().getId())
                .foundationName(l.getFoundation().getName())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .agreedConditions(l.getAgreedConditions())
                .status(l.getStatus().name())
                .transportCompanyMandatory(l.isTransportCompanyMandatory())
                .shipmentId(outbound != null ? outbound.getId() : null)
                .returnShipmentId(returnShip != null ? returnShip.getId() : null)
                .cancelledAt(l.getCancelledAt())
                .cancellationReason(l.getCancellationReason())
                .build();
    }
}
