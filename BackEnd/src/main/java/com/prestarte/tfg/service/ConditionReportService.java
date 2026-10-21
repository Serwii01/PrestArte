package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ConditionReportRequest;
import com.prestarte.tfg.model.dto.ConditionReportResponse;
import com.prestarte.tfg.model.entity.ConditionReport;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.DamageMark;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.ConditionReportRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea y consulta los informes de estado de las obras.
 *
 * Cada informe lo firma quien lo levanta y, después, la otra parte
 * presente en ese momento firma su conformidad (o su desacuerdo):
 * <ul>
 *   <li>Recogida de ida: firma el transportista, da conformidad el coleccionista.</li>
 *   <li>Entrega de ida: firma la institución, da conformidad el transportista.</li>
 *   <li>Antes de desmontar o de trasladarla a otra sede de la gira: firma la
 *       institución, da conformidad el coleccionista.</li>
 *   <li>Recogida de vuelta: firma el transportista, da conformidad la institución.</li>
 *   <li>Entrega de vuelta: firma el coleccionista, da conformidad el transportista.</li>
 * </ul>
 *
 * No comprueba quién puede crear informes: lo llaman {@link ShipmentService}
 * (que ya ha validado quién recoge o entrega) y {@link LoanRequestService}
 * (que ya ha validado el acceso al préstamo).
 */
@Service
@RequiredArgsConstructor
public class ConditionReportService {

    /** Mínimo de fotos exigido: sin fotos el informe no prueba nada. */
    public static final int MIN_PHOTOS = 1;
    public static final int MAX_PHOTOS = 10;

    private final ConditionReportRepository conditionReportRepository;
    private final UserRepository userRepository;
    private final LoanEventService loanEventService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /** Valida y guarda el informe de estado de un hito del envío. */
    @Transactional
    public ConditionReport create(Shipment shipment, ConditionReport.Stage stage,
                                  ConditionReportRequest data, List<MultipartFile> photos) throws IOException {
        if (data == null || data.getCondition() == null) {
            throw new IllegalArgumentException("Indica el estado de conservación de la obra.");
        }
        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.size() < MIN_PHOTOS) {
            throw new IllegalArgumentException("Adjunta al menos una fotografía de la obra.");
        }
        if (files.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Puedes adjuntar como máximo " + MAX_PHOTOS + " fotografías.");
        }

        List<DBFile> dbFiles = new ArrayList<>();
        for (MultipartFile f : files) {
            String type = UploadValidator.requireType(f, UploadValidator.IMAGES);
            dbFiles.add(DBFile.builder()
                    .fileName(UploadValidator.safeFileName(f.getOriginalFilename(), "foto"))
                    .fileType(type)
                    .data(f.getBytes())
                    .fileSize(f.getSize())
                    .build());
        }

        User author = userRepository.getReferenceById(currentUser.currentId());
        String notes = data.getNotes() != null && !data.getNotes().isBlank() ? data.getNotes().trim() : null;

        ConditionReport report = ConditionReport.builder()
                .shipment(shipment)
                .stage(stage)
                .condition(data.getCondition())
                .damageReported(data.isDamageReported())
                .notes(notes)
                .photos(dbFiles)
                .author(author)
                .build();

        if (data.getDamageMarks() != null) {
            for (ConditionReportRequest.Mark m : data.getDamageMarks()) {
                if (m.getPhotoIndex() >= dbFiles.size()) {
                    throw new IllegalArgumentException("Una marca de daño apunta a una foto que no existe.");
                }
                String note = m.getNote() != null && !m.getNote().isBlank() ? m.getNote().trim() : null;
                report.getMarks().add(DamageMark.builder()
                        .report(report)
                        .photo(dbFiles.get(m.getPhotoIndex()))
                        .x(m.getX())
                        .y(m.getY())
                        .note(note)
                        .build());
            }
            // Una marca de daño es, por definición, un daño señalado.
            if (!report.getMarks().isEmpty()) {
                report.setDamageReported(true);
            }
        }

        ConditionReport saved = conditionReportRepository.save(report);

        User counterpart = counterpartOf(saved);
        notificationService.notify(counterpart, "Informe de estado pendiente de tu firma",
                "Revisa el informe de estado de \"" + shipment.getLoanRequest().getArtwork().getTitle()
                        + "\" (" + stageLabel(saved) + ") y firma tu conformidad.",
                "/app/loans/" + shipment.getLoanRequest().getId(), true);
        return saved;
    }

    /** Informes de todos los envíos de un préstamo. El llamante comprueba el acceso. */
    @Transactional(readOnly = true)
    public List<ConditionReportResponse> getByLoan(Long loanId) {
        Long me = currentUser.currentIdOrNull();
        return conditionReportRepository.findByShipmentLoanRequestIdOrderByCreatedAtAscIdAsc(loanId).stream()
                .map(r -> toResponse(r, me))
                .toList();
    }

    /** True si el préstamo ya tiene el informe previo al desmontaje. */
    @Transactional(readOnly = true)
    public boolean hasPreReturnReport(Long loanId) {
        return conditionReportRepository.existsByShipmentLoanRequestIdAndStage(loanId, ConditionReport.Stage.PRE_RETURN);
    }

    /**
     * La otra parte firma el informe: conforme ({@code agree}) o en
     * desacuerdo, con un comentario obligatorio en ese caso.
     */
    @Transactional
    public ConditionReportResponse sign(LoanRequest loan, Long reportId, boolean agree, String comment) {
        ConditionReport report = conditionReportRepository.findById(reportId)
                .filter(r -> r.getShipment().getLoanRequest().getId().equals(loan.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Informe de estado no encontrado."));
        User counterpart = counterpartOf(report);
        if (!counterpart.getId().equals(currentUser.currentId())) {
            throw new AccessDeniedException("Este informe debe firmarlo " + roleLabel(counterpart) + ".");
        }
        if (report.getAcknowledgedAt() != null) {
            throw new IllegalStateException("Este informe ya está firmado.");
        }
        String cleanComment = comment != null && !comment.isBlank() ? comment.trim() : null;
        if (!agree && cleanComment == null) {
            throw new IllegalArgumentException("Explica en qué no estás conforme.");
        }

        report.setAcknowledgedBy(counterpart);
        report.setAcknowledgedAt(LocalDateTime.now());
        report.setDisputed(!agree);
        report.setAcknowledgementComment(cleanComment);
        conditionReportRepository.save(report);

        String what = "el informe de estado (" + stageLabel(report) + ")";
        if (agree) {
            loanEventService.record(loan, counterpart.getName() + " firma su conformidad con " + what
                    + (cleanComment != null ? ": " + cleanComment : "."));
        } else {
            loanEventService.recordImportant(loan, counterpart.getName() + " NO está conforme con " + what
                    + ": " + cleanComment);
        }
        return toResponse(report, counterpart.getId());
    }

    // ===== Auxiliares =====

    private ConditionReportResponse toResponse(ConditionReport r, Long viewerId) {
        User counterpart = counterpartOf(r);
        return ConditionReportResponse.builder()
                .id(r.getId())
                .shipmentId(r.getShipment().getId())
                .direction(r.getShipment().getDirection() != null
                        ? r.getShipment().getDirection().name() : null)
                .stage(r.getStage().name())
                .condition(r.getCondition().name())
                .damageReported(r.isDamageReported())
                .notes(r.getNotes())
                .authorName(r.getAuthor().getName())
                .authorRole(r.getAuthor().getRole().name())
                .photoIds(r.getPhotos().stream().map(DBFile::getId).toList())
                .createdAt(r.getCreatedAt())
                .marks(r.getMarks().stream()
                        .map(m -> new ConditionReportResponse.Mark(m.getPhoto().getId(), m.getX(), m.getY(), m.getNote()))
                        .toList())
                .counterpartRole(counterpart.getRole().name())
                .canSign(r.getAcknowledgedAt() == null && counterpart.getId().equals(viewerId))
                .acknowledgedByName(r.getAcknowledgedBy() != null ? r.getAcknowledgedBy().getName() : null)
                .acknowledgedAt(r.getAcknowledgedAt())
                .disputed(r.getDisputed())
                .acknowledgementComment(r.getAcknowledgementComment())
                .build();
    }

    /** Parte que debe firmar la conformidad del informe (ver la tabla de la clase). */
    private User counterpartOf(ConditionReport r) {
        Shipment s = r.getShipment();
        LoanRequest loan = s.getLoanRequest();
        boolean outbound = s.getDirection() != Shipment.ShipmentDirection.RETURN;
        return switch (r.getStage()) {
            case PICKUP -> outbound ? loan.getArtwork().getCollector() : loan.getFoundation();
            case DELIVERY -> s.getTransportCompany();
            case PRE_RETURN, TRANSFER -> loan.getArtwork().getCollector();
        };
    }

    private static String stageLabel(ConditionReport r) {
        boolean outbound = r.getShipment().getDirection() != Shipment.ShipmentDirection.RETURN;
        return switch (r.getStage()) {
            case PICKUP -> outbound ? "recogida de ida" : "recogida de vuelta";
            case DELIVERY -> outbound ? "entrega de ida" : "entrega de vuelta";
            case PRE_RETURN -> "antes de desmontar";
            case TRANSFER -> "antes del traslado a otra sede";
        };
    }

    private static String roleLabel(User u) {
        return switch (u.getRole().name()) {
            case "COLLECTOR" -> "el coleccionista";
            case "FOUNDATION" -> "la institución";
            case "TRANSPORT" -> "la empresa de transporte";
            default -> "la otra parte";
        };
    }
}
