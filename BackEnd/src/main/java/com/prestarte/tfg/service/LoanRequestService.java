package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.CompatibilityReport;
import com.prestarte.tfg.model.dto.ConditionReportRequest;
import com.prestarte.tfg.model.dto.ConditionReportResponse;
import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanEventResponse;
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

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Servicio que orquesta el ciclo de vida de un préstamo.
 *
 * Concentra las acciones que disparan transiciones en la máquina de
 * estados de {@link LoanRequest}: creación de la solicitud, aceptación
 * y elección de empresa de transporte, rechazos, cancelaciones,
 * reasignación de transporte tras un presupuesto rechazado, marcaje
 * de preparación, inicio y cierre del retorno. Cada acción comprueba
 * los permisos del usuario que la ejecuta, delega en
 * {@link LoanStateMachine} la validación de la transición y deja
 * constancia del hito en el historial ({@link LoanEventService}).
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
    private final LoanEventService loanEventService;
    private final ConditionReportService conditionReportService;
    private final ExhibitionService exhibitionService;
    private final FacilityReportService facilityReportService;
    private final LoanDocumentsService loanDocumentsService;
    private final ProvenanceService provenanceService;
    private final LoanStateMachine stateMachine;
    private final CurrentUser currentUser;

    // ===== Creación =====

    /**
     * Crea una nueva solicitud de préstamo en estado REQUESTED.
     *
     * La acción la ejecuta la propia fundación; el método comprueba
     * que la obra exista, esté disponible para préstamo y no esté ya
     * comprometida en esas fechas, y que la fundación no tenga ya otra
     * solicitud pendiente para la misma obra y fechas.
     */
    @Transactional
    public LoanResponse createRequest(CreateLoanRequest dto) {
        currentUser.requireUserId(dto.getFoundationId());

        Artwork artwork = artworkRepository.findById(dto.getArtworkId())
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", dto.getArtworkId()));

        provenanceService.requireLendable(artwork);
        if (!artwork.isAvailableForLoan()) {
            throw new IllegalStateException(
                    "Esta obra no está disponible para préstamo en este momento.");
        }
        if (dto.getEndDate() == null || dto.getStartDate() == null
                || !dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio.");
        }
        if (loanRequestRepository.existsOverlappingLoan(
                artwork.getId(), null, dto.getStartDate(), dto.getEndDate())) {
            throw new IllegalStateException(
                    "La obra ya está reservada para otro préstamo en esas fechas.");
        }
        boolean duplicate = loanRequestRepository.findOverlapping(
                        artwork.getId(), -1L, dto.getStartDate(), dto.getEndDate(),
                        EnumSet.of(LoanRequest.Status.REQUESTED))
                .stream()
                .anyMatch(l -> l.getFoundation().getId().equals(dto.getFoundationId()));
        if (duplicate) {
            throw new IllegalStateException(
                    "Ya tienes una solicitud pendiente para esta obra en esas fechas.");
        }

        Exhibition exhibition = exhibitionService.getForLoanRequest(dto.getExhibitionId(), dto.getFoundationId());
        // En una exposición itinerante, el préstamo cubre la gira entera.
        LocalDate tourEnd = exhibition.tourClosingDate();
        if (dto.getStartDate().isAfter(exhibition.getOpeningDate())
                || dto.getEndDate().isBefore(tourEnd)) {
            throw new IllegalArgumentException(
                    "El préstamo debe cubrir toda la exposición" + (exhibition.isItinerant() ? ", con todas sus sedes" : "")
                            + " (del " + exhibition.getOpeningDate() + " al " + tourEnd
                            + "), con margen para transporte y montaje.");
        }

        LoanRequirements req = artwork.getRequirements();
        if (req != null && req.getMinNoticeDays() != null) {
            long notice = ChronoUnit.DAYS.between(LocalDate.now(), dto.getStartDate());
            if (notice < req.getMinNoticeDays()) {
                throw new IllegalArgumentException(
                        "Esta obra debe solicitarse con al menos " + req.getMinNoticeDays()
                                + " días de antelación respecto al inicio del préstamo.");
            }
        }

        Foundation foundation = foundationRepository.findById(dto.getFoundationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Fundación", dto.getFoundationId()));

        LoanRequest request = loanRequestRepository.save(LoanRequest.builder()
                .artwork(artwork)
                .foundation(foundation)
                .exhibition(exhibition)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .agreedConditions(dto.getAgreedConditions())
                .status(LoanRequest.Status.REQUESTED)
                .transportCompanyMandatory(false)
                .build());

        loanEventService.recordTransition(request, null, LoanRequest.Status.REQUESTED,
                foundation.getName() + " solicita el préstamo para la exposición «"
                        + exhibition.getTitle() + "» (" + exhibition.getVenueName() + ", "
                        + exhibition.getVenueCity()
                        + (exhibition.isItinerant() ? " y " + exhibition.getStops().size() + " sede(s) más" : "")
                        + "), del " + dto.getStartDate()
                        + " al " + dto.getEndDate() + ".");
        return convertToResponse(request);
    }

    // ===== Acciones del flujo (dispara el coleccionista o la fundación) =====

    /**
     * El coleccionista acepta la solicitud y elige la empresa de
     * transporte. Tras la aceptación se crea de inmediato el envío
     * OUTBOUND en estado REQUESTED y el préstamo avanza a
     * QUOTE_PENDING, a la espera del presupuesto.
     *
     * Antes de aceptar se comprueba que la obra no esté comprometida
     * con otro préstamo en las mismas fechas. Después, el resto de
     * solicitudes pendientes que se solapan se rechazan automáticamente
     * para que esas fundaciones no se queden esperando.
     */
    @Transactional
    public LoanResponse accept(Long loanId, Long transportCompanyId, boolean mandatory, Double agreedValue) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());

        boolean overlapping = loanRequestRepository.existsOverlappingLoan(
                loan.getArtwork().getId(), loan.getId(), loan.getStartDate(), loan.getEndDate());
        if (overlapping) {
            throw new IllegalStateException(
                    "Esta obra ya está reservada para las fechas seleccionadas.");
        }

        TransportCompany company = transportCompanyRepository.findById(transportCompanyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Empresa de transporte", transportCompanyId));

        loan.setTransportCompanyMandatory(mandatory);
        // El valor asegurado acordado: el que indique el coleccionista o, si no,
        // el valor estimado de la obra. Ningún seguro podrá cubrir menos.
        loan.setAgreedValue(agreedValue != null ? agreedValue : loan.getArtwork().getEstimatedValue());
        transition(loan, LoanRequest.Status.ACCEPTED,
                "El coleccionista acepta el préstamo con un valor asegurado de "
                        + euros(loan.getAgreedValue()) + " y elige a " + company.getCompanyName()
                        + (mandatory ? " como transportista obligatorio." : " como transportista."));

        shipmentService.createOutboundShipment(loan, company);
        transition(loan, LoanRequest.Status.QUOTE_PENDING,
                "Se solicita presupuesto a " + company.getCompanyName() + ".");

        rejectOverlappingRequests(loan);
        return convertToResponse(loan);
    }

    /** El coleccionista rechaza la solicitud de manera definitiva. */
    @Transactional
    public LoanResponse reject(Long loanId, String reason) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        loan.setRejectionReason(cleanReason);
        transition(loan, LoanRequest.Status.REJECTED,
                "El coleccionista rechaza la solicitud"
                        + (cleanReason != null ? ": " + cleanReason : "."));
        return convertToResponse(loan);
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
        loanEventService.record(loan, "Se asigna una nueva empresa de transporte: "
                + company.getCompanyName() + ". Se le solicita presupuesto.");
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
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        loan.setCancelledAt(LocalDateTime.now());
        loan.setCancellationReason(cleanReason);
        boolean byCollector = currentUser.isAnyOf(loan.getArtwork().getCollector().getId());
        transition(loan, LoanRequest.Status.CANCELLED,
                (byCollector ? "El coleccionista" : "La fundación") + " cancela el préstamo"
                        + (cleanReason != null ? ": " + cleanReason : "."));
        return convertToResponse(loan);
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
        loanDocumentsService.requireReadyForShipment(loan);
        transition(loan, LoanRequest.Status.READY_FOR_PICKUP,
                "El coleccionista indica que la obra está lista para su recogida.");
        return convertToResponse(loan);
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
        if (!conditionReportService.hasPreReturnReport(loanId)) {
            throw new IllegalStateException(
                    "Antes de iniciar la devolución, levanta el informe de estado previo al desmontaje.");
        }
        transition(loan, LoanRequest.Status.RETURNING,
                "La fundación inicia la devolución de la obra.");

        shipmentService.createReturnShipment(loan);
        return convertToResponse(loan);
    }

    // ===== Ampliación del préstamo =====

    /** La fundación pide ampliar el préstamo mientras la obra está expuesta. */
    @Transactional
    public LoanResponse requestExtension(Long loanId, LocalDate newEnd, String reason) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getFoundation().getId());
        if (loan.getStatus() != LoanRequest.Status.ON_LOAN) {
            throw new IllegalStateException("Solo se puede ampliar un préstamo con la obra expuesta.");
        }
        if (loan.getRequestedEndDate() != null) {
            throw new IllegalStateException("Ya hay una ampliación pendiente de respuesta.");
        }
        if (!newEnd.isAfter(loan.getEndDate())) {
            throw new IllegalArgumentException("La nueva fecha debe ser posterior a la actual (" + loan.getEndDate() + ").");
        }
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        loan.setRequestedEndDate(newEnd);
        loan.setExtensionReason(cleanReason);
        loanRequestRepository.save(loan);
        loanEventService.recordImportant(loan, "La fundación pide ampliar el préstamo hasta el " + newEnd
                + (cleanReason != null ? ": " + cleanReason : "."));
        return convertToResponse(loan);
    }

    /**
     * El coleccionista aprueba la ampliación. Se comprueba que la obra no
     * esté comprometida después y que el permiso de exportación lo permita.
     */
    @Transactional
    public LoanResponse approveExtension(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        LocalDate newEnd = loan.getRequestedEndDate();
        if (newEnd == null) {
            throw new IllegalStateException("No hay ninguna ampliación pendiente.");
        }
        boolean clash = loanRequestRepository.existsOverlappingLoan(
                loan.getArtwork().getId(), loan.getId(), loan.getEndDate(), newEnd);
        if (clash) {
            throw new IllegalStateException("La obra ya está comprometida con otro préstamo en esas fechas.");
        }
        List<String> warnings = loanDocumentsService.extensionWarnings(loan, newEnd);

        LocalDate oldEnd = loan.getEndDate();
        loan.setEndDate(newEnd);
        loan.setRequestedEndDate(null);
        loan.setExtensionReason(null);
        loanRequestRepository.save(loan);
        loanEventService.recordImportant(loan, "El coleccionista aprueba ampliar el préstamo del " + oldEnd
                + " al " + newEnd + "." + (warnings.isEmpty() ? "" : " " + String.join(" ", warnings)));
        return convertToResponse(loan);
    }

    @Transactional
    public LoanResponse rejectExtension(Long loanId, String reason) {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        if (loan.getRequestedEndDate() == null) {
            throw new IllegalStateException("No hay ninguna ampliación pendiente.");
        }
        LocalDate asked = loan.getRequestedEndDate();
        loan.setRequestedEndDate(null);
        loan.setExtensionReason(null);
        loanRequestRepository.save(loan);
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        loanEventService.recordImportant(loan, "El coleccionista no acepta ampliar el préstamo hasta el " + asked
                + (cleanReason != null ? ": " + cleanReason : ". Se mantiene la fecha de fin del " + loan.getEndDate() + "."));
        return convertToResponse(loan);
    }

    /**
     * Cierra el ciclo del préstamo cuando el envío de retorno se ha
     * entregado al coleccionista. La llama {@link ShipmentService} al
     * confirmar la entrega del envío RETURN.
     */
    @Transactional
    public void onReturnDelivered(LoanRequest loan, String receivedBy) {
        transition(loan, LoanRequest.Status.RETURNED,
                "El coleccionista confirma que la obra ha vuelto"
                        + (receivedBy != null ? " (recibida por " + receivedBy + ")" : "")
                        + ". Préstamo cerrado.");
    }

    // ===== Sincronizaciones disparadas desde ShipmentService =====

    /** Avanza el préstamo a QUOTE_PROPOSED cuando el transportista sube su presupuesto. */
    @Transactional
    public void onShipmentQuoted(LoanRequest loan, Shipment shipment) {
        transition(loan, LoanRequest.Status.QUOTE_PROPOSED,
                shipment.getTransportCompany().getCompanyName() + " envía su presupuesto: "
                        + euros(shipment.getPrice()) + " de ida, "
                        + euros(shipment.getReturnPrice()) + " de vuelta y "
                        + euros(shipment.getInsuranceCost()) + " de seguro.");
    }

    /**
     * Avanza el préstamo a PAID cuando se aprueba el presupuesto del
     * envío. Tras la transición notifica a las tres partes con el
     * contrato definitivo en PDF.
     */
    @Transactional
    public void onPaymentSucceeded(LoanRequest loan) {
        transition(loan, LoanRequest.Status.PAID,
                "La fundación aprueba el presupuesto. Contrato formalizado y enviado a las partes.");
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
        String cleanReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        String suffix = cleanReason != null ? ": " + cleanReason : ".";
        if (loan.isTransportCompanyMandatory()) {
            loan.setCancelledAt(LocalDateTime.now());
            loan.setCancellationReason(cleanReason != null ? cleanReason
                    : "Presupuesto rechazado y la empresa de transporte era obligatoria.");
            transition(loan, LoanRequest.Status.CANCELLED,
                    "La fundación rechaza el presupuesto y, como el transportista era obligatorio, "
                            + "el préstamo se cancela" + suffix);
        } else {
            transition(loan, LoanRequest.Status.QUOTE_PENDING,
                    "La fundación rechaza el presupuesto" + suffix);
        }
    }

    /** Avanza el préstamo a IN_TRANSIT cuando el transportista confirma la recogida. */
    @Transactional
    public void onShipmentPickedUp(LoanRequest loan, Shipment shipment) {
        transition(loan, LoanRequest.Status.IN_TRANSIT,
                shipment.getTransportCompany().getCompanyName()
                        + " recoge la obra y levanta el informe de estado de salida.");
    }

    /**
     * Avanza el préstamo a DELIVERED cuando el museo confirma la
     * recepción, y a continuación lo deja en ON_LOAN para reflejar
     * que la obra ya se encuentra en exposición.
     */
    @Transactional
    public void onShipmentDelivered(LoanRequest loan, String receivedBy) {
        transition(loan, LoanRequest.Status.DELIVERED,
                "La fundación confirma la recepción de la obra"
                        + (receivedBy != null ? " (recibida por " + receivedBy + ")" : "")
                        + " y levanta el informe de estado de llegada.");
        transition(loan, LoanRequest.Status.ON_LOAN, "La obra queda en préstamo en la institución.");
    }

    /** Registra en el historial un hito del envío que no cambia el estado del préstamo. */
    @Transactional
    public void recordShipmentEvent(LoanRequest loan, String message) {
        loanEventService.record(loan, message);
    }

    /** Registra una incidencia del envío (daños): avisa a las partes también por email. */
    @Transactional
    public void recordShipmentIncident(LoanRequest loan, String message) {
        loanEventService.recordImportant(loan, message);
    }

    // ===== Historial e informes de estado =====

    /** Historial del préstamo, si la sesión actual puede consultarlo. */
    @Transactional(readOnly = true)
    public List<LoanEventResponse> getHistory(Long loanId) {
        requireLoanAccess(findOrThrow(loanId));
        return loanEventService.getHistory(loanId);
    }

    /**
     * Compatibilidad entre los requisitos de la obra y las instalaciones
     * de la fundación que la pide, si la sesión actual puede consultar el
     * préstamo. Es lo que ve el coleccionista antes de aceptar.
     */
    @Transactional(readOnly = true)
    public CompatibilityReport getCompatibility(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        requireLoanAccess(loan);
        return facilityReportService.checkLoan(loan);
    }

    /**
     * Certificado de exposición en PDF. Lo pueden descargar el coleccionista,
     * la fundación y la administración una vez que la obra ha llegado a la
     * exposición.
     */
    @Transactional(readOnly = true)
    public byte[] getExhibitionCertificate(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        if (!currentUser.isAdmin()) {
            currentUser.requireAnyUserId(loan.getArtwork().getCollector().getId(), loan.getFoundation().getId());
        }
        if (loan.getExhibition() == null) {
            throw new IllegalStateException("Este préstamo no está asociado a ninguna exposición.");
        }
        if (!ExhibitionService.EXHIBITED.contains(loan.getStatus())) {
            throw new IllegalStateException("El certificado está disponible cuando la obra ha llegado a la exposición.");
        }
        return pdfGeneratorService.generateExhibitionCertificate(loan);
    }

    /** Informes de estado del préstamo, si la sesión actual puede consultarlo. */
    @Transactional(readOnly = true)
    public List<ConditionReportResponse> getConditionReports(Long loanId) {
        requireLoanAccess(findOrThrow(loanId));
        return conditionReportService.getByLoan(loanId);
    }

    /**
     * La fundación documenta el estado de la obra en sala, antes de
     * desmontarla. Es obligatorio para iniciar la devolución: separa los
     * daños sufridos durante la exposición de los del viaje de vuelta.
     */
    @Transactional
    public List<ConditionReportResponse> createPreReturnReport(Long loanId, ConditionReportRequest data,
                                                               List<MultipartFile> photos) throws IOException {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getFoundation().getId());
        if (loan.getStatus() != LoanRequest.Status.ON_LOAN) {
            throw new IllegalStateException("El informe previo al desmontaje se hace con la obra expuesta.");
        }
        if (conditionReportService.hasPreReturnReport(loanId)) {
            throw new IllegalStateException("Ya existe un informe previo al desmontaje.");
        }
        if (remainingStops(loan) > 0) {
            throw new IllegalStateException("La obra aún tiene sedes pendientes en la gira: regístrala como "
                    + "traslado a la siguiente sede, no como desmontaje final.");
        }
        Shipment outbound = shipmentRepository.findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loanId, Shipment.ShipmentDirection.OUTBOUND)
                .orElseThrow(() -> new IllegalStateException("El préstamo no tiene envío de ida."));
        conditionReportService.create(outbound, ConditionReport.Stage.PRE_RETURN, data, photos);
        loanEventService.record(loan, "La fundación levanta el informe de estado previo al desmontaje"
                + (data.isDamageReported() || (data.getDamageMarks() != null && !data.getDamageMarks().isEmpty())
                ? ", con daños señalados." : "."));
        return conditionReportService.getByLoan(loanId);
    }

    /**
     * Exposición itinerante: la fundación documenta el estado de la obra
     * antes de desmontarla y la traslada a la siguiente sede de la gira.
     * El préstamo sigue ON_LOAN; el traslado queda en el historial y el
     * coleccionista firma la conformidad del informe.
     */
    @Transactional
    public LoanResponse transferToNextStop(Long loanId, ConditionReportRequest data,
                                           List<MultipartFile> photos) throws IOException {
        LoanRequest loan = findOrThrow(loanId);
        currentUser.requireUserId(loan.getFoundation().getId());
        if (loan.getStatus() != LoanRequest.Status.ON_LOAN) {
            throw new IllegalStateException("Solo se traslada una obra que está expuesta.");
        }
        if (remainingStops(loan) == 0) {
            throw new IllegalStateException("No quedan sedes en la gira: la siguiente etapa es la devolución.");
        }
        Shipment outbound = shipmentRepository.findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        loanId, Shipment.ShipmentDirection.OUTBOUND)
                .orElseThrow(() -> new IllegalStateException("El préstamo no tiene envío de ida."));
        conditionReportService.create(outbound, ConditionReport.Stage.TRANSFER, data, photos);

        Exhibition ex = loan.getExhibition();
        int from = loan.getCurrentStop() != null ? loan.getCurrentStop() : 0;
        loan.setCurrentStop(from + 1);
        loanRequestRepository.save(loan);
        loanEventService.recordImportant(loan, "La obra se traslada de " + ex.venueLabel(from)
                + " a " + ex.venueLabel(from + 1) + " (" + ex.getStops().get(from).getHostInstitution() + ")."
                + (data.isDamageReported() ? " El informe de estado previo señala daños." : ""));
        return convertToResponse(loan);
    }

    /** Sedes de la gira que la obra aún no ha visitado. */
    private static int remainingStops(LoanRequest loan) {
        Exhibition ex = loan.getExhibition();
        if (ex == null || !ex.isItinerant()) return 0;
        int current = loan.getCurrentStop() != null ? loan.getCurrentStop() : 0;
        return Math.max(0, ex.getStops().size() - current);
    }

    /** La otra parte firma su conformidad (o no) con un informe de estado. */
    @Transactional
    public ConditionReportResponse signConditionReport(Long loanId, Long reportId, boolean agree, String comment) {
        LoanRequest loan = findOrThrow(loanId);
        requireLoanAccess(loan);
        return conditionReportService.sign(loan, reportId, agree, comment);
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

    /**
     * Valida la transición con la máquina de estados, la aplica y la
     * deja registrada en el historial con el usuario de la sesión.
     */
    private void transition(LoanRequest loan, LoanRequest.Status to, String message) {
        LoanRequest.Status from = loan.getStatus();
        stateMachine.validate(from, to);
        loan.setStatus(to);
        loanRequestRepository.save(loan);
        loanEventService.recordTransition(loan, from, to, message);
    }

    /**
     * Rechaza automáticamente las solicitudes pendientes de la misma obra
     * cuyas fechas se solapan con el préstamo recién aceptado, y avisa por
     * email a las fundaciones afectadas.
     */
    private void rejectOverlappingRequests(LoanRequest accepted) {
        List<LoanRequest> pending = loanRequestRepository.findOverlapping(
                accepted.getArtwork().getId(), accepted.getId(),
                accepted.getStartDate(), accepted.getEndDate(),
                EnumSet.of(LoanRequest.Status.REQUESTED));

        String reason = "La obra se ha reservado para otra institución en fechas que coinciden con tu solicitud.";
        for (LoanRequest other : pending) {
            LoanRequest.Status from = other.getStatus();
            stateMachine.validate(from, LoanRequest.Status.REJECTED);
            other.setStatus(LoanRequest.Status.REJECTED);
            other.setRejectionReason(reason);
            loanRequestRepository.save(other);
            loanEventService.recordSystemTransition(other, from, LoanRequest.Status.REJECTED,
                    "Solicitud rechazada automáticamente: " + reason);

            String email = other.getFoundation().getEmail();
            if (email != null) {
                emailService.sendSimpleEmail(email,
                        "Solicitud de préstamo no disponible: " + other.getArtwork().getTitle(),
                        "Hola " + other.getFoundation().getName() + ",\n\n" +
                        "Tu solicitud de préstamo de \"" + other.getArtwork().getTitle() + "\" del " +
                        other.getStartDate() + " al " + other.getEndDate() + " ya no puede atenderse: " +
                        reason + "\n\nPuedes solicitarla para otras fechas desde el catálogo.\n\n" +
                        "Un saludo,\nEl equipo de Prestarte.");
            }
        }
        if (!pending.isEmpty()) {
            log.info("Préstamo {} aceptado: {} solicitudes solapadas rechazadas automáticamente.",
                    accepted.getId(), pending.size());
        }
    }

    private static String euros(Double amount) {
        return amount == null ? "—" : String.format(Locale.forLanguageTag("es-ES"), "%,.2f €", amount);
    }

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

        // Si la obra es anónima, la identidad del coleccionista solo se
        // revela a terceros cuando acepta el préstamo (existe envío).
        Long collectorId = l.getArtwork().getCollector().getId();
        boolean revealOwner = l.getArtwork().isShowOwnerName()
                || outbound != null
                || currentUser.isAdminOrNull()
                || collectorId.equals(currentUser.currentIdOrNull());

        return LoanResponse.builder()
                .id(l.getId())
                .artworkId(l.getArtwork().getId())
                .artworkTitle(l.getArtwork().getTitle())
                .artworkArtist(l.getArtwork().getArtist())
                .exhibitionId(l.getExhibition() != null ? l.getExhibition().getId() : null)
                .exhibitionTitle(l.getExhibition() != null ? l.getExhibition().getTitle() : null)
                .exhibitionVenue(l.getExhibition() != null
                        ? l.getExhibition().getVenueName() + ", " + l.getExhibition().getVenueCity() : null)
                .exhibitionOpeningDate(l.getExhibition() != null ? l.getExhibition().getOpeningDate() : null)
                .exhibitionClosingDate(l.getExhibition() != null ? l.getExhibition().getClosingDate() : null)
                .exhibitionStops(l.getExhibition() != null ? ExhibitionService.stopsOf(l.getExhibition()) : List.of())
                .currentStop(l.getCurrentStop() != null ? l.getCurrentStop() : 0)
                .collectorId(revealOwner ? collectorId : null)
                .collectorName(revealOwner ? l.getArtwork().getCollector().getName()
                        : ArtworkService.ANONYMOUS_OWNER)
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
                .rejectionReason(l.getRejectionReason())
                .agreedValue(l.getAgreedValue())
                .requestedEndDate(l.getRequestedEndDate())
                .extensionReason(l.getExtensionReason())
                .build();
    }
}
