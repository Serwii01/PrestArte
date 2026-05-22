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

/**
 * Servicio que orquesta el ciclo de vida de un préstamo.
 *
 * Concentra las acciones que disparan transiciones en la máquina de
 * estados de {@link LoanRequest}: creación de la solicitud, aceptación
 * y elección de empresa de transporte, rechazos, cancelaciones,
 * reasignación de transporte tras un presupuesto rechazado, marcaje
 * de preparación, inicio y cierre del retorno. Cada acción comprueba
 * los permisos del usuario que la ejecuta y delega en
 * {@link LoanStateMachine} la validación de la transición.
 */
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

    // ===== Creación =====

    /**
     * Crea una nueva solicitud de préstamo en estado REQUESTED.
     *
     * La acción la ejecuta la propia fundación; el método comprueba
     * que la obra exista y esté disponible para préstamo antes de
     * persistir la solicitud.
     */
    @Transactional
    public LoanResponse createRequest(CreateLoanRequest dto) {
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

    // ===== Acciones del flujo (dispara el coleccionista o la fundación) =====

    /**
     * El coleccionista acepta la solicitud y elige la empresa de
     * transporte. Tras la aceptación se crea de inmediato el envío
     * OUTBOUND en estado REQUESTED y el préstamo avanza a
     * QUOTE_PENDING, a la espera del presupuesto. Antes de aceptar se
     * comprueba que la obra no tenga otro préstamo aceptado en las
     * mismas fechas.
     */
    @Transactional
    public LoanResponse accept(Long loanId, Long transportCompanyId, boolean mandatory) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());

        boolean overlapping = loanRequestRepository.existsOverlappingLoan(
                loan.getArtwork().getId(), loan.getStartDate(), loan.getEndDate());
        if (overlapping) {
            throw new IllegalStateException(
                    "Esta obra ya está reservada para las fechas seleccionadas.");
        }

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", transportCompanyId));

        stateMachine.validate(loan.getStatus(), LoanRequest.Status.ACCEPTED);
        loan.setStatus(LoanRequest.Status.ACCEPTED);
        loan.setTransportCompanyMandatory(mandatory);
        loanRequestRepository.save(loan);

        shipmentService.createOutboundShipment(loan, company);
        stateMachine.validate(LoanRequest.Status.ACCEPTED, LoanRequest.Status.QUOTE_PENDING);
        loan.setStatus(LoanRequest.Status.QUOTE_PENDING);

        return convertToResponse(loanRequestRepository.save(loan));
    }

    /** El coleccionista rechaza la solicitud de manera definitiva. */
    @Transactional
    public LoanResponse reject(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.REJECTED);
        loan.setStatus(LoanRequest.Status.REJECTED);
        return convertToResponse(loanRequestRepository.save(loan));
    }

    /**
     * Asigna una nueva empresa de transporte después de que el museo
     * haya rechazado el presupuesto anterior.
     *
     * Solo aplica cuando el préstamo está en QUOTE_PENDING, la empresa
     * inicial no era obligatoria y el último envío OUTBOUND quedó en
     * estado REJECTED. La acción puede iniciarla la fundación o el
     * coleccionista. Como resultado se crea un nuevo Shipment OUTBOUND
     * con la empresa elegida; el préstamo permanece en QUOTE_PENDING a
     * la espera del nuevo presupuesto.
     */
    @Transactional
    public LoanResponse reassignTransport(Long loanId, Long newTransportCompanyId) {
        LoanRequest loan = findOrThrow(loanId);
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

        if (lastOutbound.getTransportCompany().getId().equals(newTransportCompanyId)) {
            throw new IllegalStateException(
                    "Selecciona una empresa distinta a la que acaba de rechazar el presupuesto.");
        }

        shipmentService.createOutboundShipment(loan, company);
        return convertToResponse(loan);
    }

    /**
     * Cancela el préstamo. La acción la puede iniciar tanto el
     * coleccionista como la fundación, siempre que el estado actual
     * permita la transición a CANCELLED.
     */
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

    /**
     * El coleccionista indica que la obra está preparada para ser
     * recogida. Marca la transición a READY_FOR_PICKUP cuando el
     * presupuesto ya está aprobado.
     */
    @Transactional
    public LoanResponse markReadyForPickup(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.READY_FOR_PICKUP);
        loan.setStatus(LoanRequest.Status.READY_FOR_PICKUP);
        return convertToResponse(loanRequestRepository.save(loan));
    }

    /**
     * El museo inicia la devolución. Avanza el préstamo a RETURNING y
     * crea automáticamente un envío de retorno asociado con la misma
     * empresa de transporte; el coste de la devolución se considera
     * incluido en el presupuesto original.
     */
    @Transactional
    public LoanResponse startReturn(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getFoundation().getId());
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.RETURNING);
        loan.setStatus(LoanRequest.Status.RETURNING);
        LoanRequest saved = loanRequestRepository.save(loan);

        shipmentService.createReturnShipment(saved);
        return convertToResponse(saved);
    }

    /**
     * Cierra el ciclo del préstamo cuando el envío de retorno se ha
     * entregado al coleccionista. La llama {@link ShipmentService} al
     * confirmar la entrega del envío RETURN.
     */
    @Transactional
    public void onReturnDelivered(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.RETURNED);
        loan.setStatus(LoanRequest.Status.RETURNED);
        loanRequestRepository.save(loan);
    }

    // ===== Sincronizaciones disparadas desde ShipmentService =====

    /** Avanza el préstamo a QUOTE_PROPOSED cuando el transportista sube su presupuesto. */
    @Transactional
    public void onShipmentQuoted(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.QUOTE_PROPOSED);
        loan.setStatus(LoanRequest.Status.QUOTE_PROPOSED);
        loanRequestRepository.save(loan);
    }

    /**
     * Avanza el préstamo a PAID cuando se aprueba el presupuesto del
     * envío. Tras la transición notifica a las tres partes con el
     * contrato definitivo en PDF.
     */
    @Transactional
    public void onPaymentSucceeded(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.PAID);
        loan.setStatus(LoanRequest.Status.PAID);
        loanRequestRepository.save(loan);
        notifyPartiesOnAcceptance(loan);
    }

    /**
     * Reacciona al rechazo de un presupuesto por parte del museo. Si
     * la empresa de transporte era obligatoria, el préstamo se
     * cancela. En caso contrario, vuelve a QUOTE_PENDING para que se
     * pueda reasignar a otra empresa.
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

    /** Avanza el préstamo a IN_TRANSIT cuando el transportista confirma la recogida. */
    @Transactional
    public void onShipmentPickedUp(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.IN_TRANSIT);
        loan.setStatus(LoanRequest.Status.IN_TRANSIT);
        loanRequestRepository.save(loan);
    }

    /**
     * Avanza el préstamo a DELIVERED cuando el museo confirma la
     * recepción, y a continuación lo deja en ON_LOAN para reflejar
     * que la obra ya se encuentra en exposición.
     */
    @Transactional
    public void onShipmentDelivered(LoanRequest loan) {
        stateMachine.validate(loan.getStatus(), LoanRequest.Status.DELIVERED);
        loan.setStatus(LoanRequest.Status.DELIVERED);
        loanRequestRepository.save(loan);
        stateMachine.validate(LoanRequest.Status.DELIVERED, LoanRequest.Status.ON_LOAN);
        loan.setStatus(LoanRequest.Status.ON_LOAN);
        loanRequestRepository.save(loan);
    }

    // ===== Lecturas =====

    /** Devuelve la ficha de un préstamo si la sesión actual puede consultarlo. */
    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        LoanRequest loan = findOrThrow(id);
        requireLoanAccess(loan);
        return convertToResponse(loan);
    }

    /**
     * Devuelve la entidad sin convertir a DTO. Resulta útil para
     * componer documentos como el contrato en PDF, que necesitan
     * acceso a relaciones perezosas dentro de la misma transacción.
     */
    @Transactional(readOnly = true)
    public LoanRequest getEntityById(Long id) {
        LoanRequest loan = findOrThrow(id);
        requireLoanAccess(loan);
        loan.getArtwork().getCollector().getName();
        loan.getFoundation().getName();
        return loan;
    }

    /** Devuelve los préstamos solicitados por una fundación concreta. */
    @Transactional(readOnly = true)
    public List<LoanResponse> getRequestsByFoundation(Long foundationId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(foundationId);
        }
        return loanRequestRepository.findByFoundationId(foundationId).stream()
                .map(this::convertToResponse).toList();
    }

    /** Devuelve los préstamos cuyas obras pertenecen a un coleccionista. */
    @Transactional(readOnly = true)
    public List<LoanResponse> getRequestsByCollector(Long collectorId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(collectorId);
        }
        return loanRequestRepository.findByArtworkCollectorId(collectorId).stream()
                .map(this::convertToResponse).toList();
    }

    /** Devuelve todos los préstamos del sistema. Reservado al administrador. */
    @Transactional(readOnly = true)
    public List<LoanResponse> getAllLoanRequests() {
        if (!currentUser.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo los administradores pueden listar todos los préstamos");
        }
        return loanRequestRepository.findAll().stream()
                .map(this::convertToResponse).toList();
    }

    // ===== Helpers privados =====

    private LoanRequest findOrThrow(Long id) {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", id));
    }

    /**
     * Verifica que el usuario actual puede consultar el préstamo. Es
     * accesible para el coleccionista dueño, la fundación solicitante,
     * cualquier empresa de transporte que haya participado en él y
     * los administradores.
     */
    private void requireLoanAccess(LoanRequest loan) {
        if (currentUser.isAdmin()) return;
        Long collectorId = loan.getArtwork().getCollector().getId();
        Long foundationId = loan.getFoundation().getId();
        if (currentUser.isAnyOf(collectorId, foundationId)) return;
        boolean isTransport = shipmentRepository.findByTransportCompanyId(currentUser.currentId())
                .stream()
                .anyMatch(s -> loan.getId().equals(s.getLoanRequest().getId()));
        if (!isTransport) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes acceso a este préstamo");
        }
    }

    /**
     * Genera el contrato del préstamo en PDF y lo envía por correo
     * electrónico a la fundación, al coleccionista y a la empresa de
     * transporte. Cualquier fallo en el envío se registra en el log
     * sin interrumpir el avance del préstamo.
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

    /**
     * Compone el DTO de respuesta de un préstamo. Para los envíos se
     * elige siempre el más reciente de cada dirección, ya que tras una
     * reasignación de transporte pueden existir varios envíos OUTBOUND
     * y solo el último es el que está vivo en el flujo.
     */
    private LoanResponse convertToResponse(LoanRequest l) {
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
