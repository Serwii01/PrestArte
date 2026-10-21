package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ExportPermitRequest;
import com.prestarte.tfg.model.dto.InsuranceCertificateRequest;
import com.prestarte.tfg.model.dto.LoanDocumentsResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.model.entity.InsuranceCertificate.CoverageType;
import com.prestarte.tfg.model.entity.InsuranceCertificate.ReviewStatus;
import com.prestarte.tfg.repository.ExportPermitRepository;
import com.prestarte.tfg.repository.InsuranceCertificateRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Documentación obligatoria antes de que una obra salga de casa: seguro
 * "clavo a clavo" y, si hace falta, permiso de exportación temporal.
 *
 * Reglas:
 * <ul>
 *   <li>El seguro lo aporta la fundación (póliza propia o Garantía del
 *       Estado) y el coleccionista lo aprueba o rechaza; si el
 *       coleccionista asegura con su propia póliza, la sube él y queda
 *       aprobada. Debe cubrir el valor acordado y todo el periodo del
 *       préstamo.</li>
 *   <li>El permiso de exportación hace falta si la obra sale de España y
 *       tiene 100 años o más, o es BIC o está inventariada. Lo puede subir
 *       cualquiera de las dos partes y su fecha límite de retorno debe ser
 *       posterior al fin del préstamo.</li>
 * </ul>
 * Sin esta documentación ni el coleccionista puede marcar la obra como
 * lista para recoger ni el transportista recogerla
 * ({@link #requireReadyForShipment}).
 */
@Service
@RequiredArgsConstructor
public class LoanDocumentsService {

    /** Antigüedad a partir de la cual una obra necesita permiso para salir de España. */
    private static final int HERITAGE_AGE_YEARS = 100;

    /** Estados en los que el préstamo está aceptado y la obra aún no ha salido o está fuera. */
    private static final Set<LoanRequest.Status> DOCUMENT_STATUSES = EnumSet.of(
            LoanRequest.Status.ACCEPTED,
            LoanRequest.Status.QUOTE_PENDING,
            LoanRequest.Status.QUOTE_PROPOSED,
            LoanRequest.Status.PAID,
            LoanRequest.Status.READY_FOR_PICKUP);

    private final LoanRequestRepository loanRequestRepository;
    private final InsuranceCertificateRepository insuranceRepository;
    private final ExportPermitRepository permitRepository;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;
    private final LoanEventService loanEventService;
    private final CurrentUser currentUser;

    // ===== Consulta =====

    @Transactional(readOnly = true)
    public LoanDocumentsResponse getStatus(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        chatSessionService.requireParticipant(loan);
        return buildStatus(loan);
    }

    /**
     * Lanza una excepción con lo que falta si la obra todavía no puede
     * salir. La usan el marcado de "lista para recoger" y la recogida.
     */
    @Transactional(readOnly = true)
    public void requireReadyForShipment(LoanRequest loan) {
        List<String> missing = missing(loan);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("La obra todavía no puede salir: " + String.join(" ", missing));
        }
    }

    // ===== Seguro =====

    @Transactional
    public LoanDocumentsResponse uploadInsurance(Long loanId, InsuranceCertificateRequest data,
                                                 MultipartFile file) throws IOException {
        LoanRequest loan = findOrThrow(loanId);
        requireDocumentPhase(loan);
        Long collectorId = loan.getArtwork().getCollector().getId();
        Long foundationId = loan.getFoundation().getId();

        boolean byCollector = currentUser.isAnyOf(collectorId);
        if (data.getCoverageType() == CoverageType.LENDER_POLICY) {
            if (!byCollector) {
                throw new AccessDeniedException("La póliza del prestador la sube el propio coleccionista.");
            }
        } else {
            currentUser.requireUserId(foundationId);
        }

        double required = agreedValue(loan);
        if (data.getInsuredAmount() < required) {
            throw new IllegalArgumentException("La suma asegurada (" + euros(data.getInsuredAmount())
                    + ") es inferior al valor acordado (" + euros(required) + ").");
        }
        if (data.getCoverageStart().isAfter(loan.getStartDate()) || data.getCoverageEnd().isBefore(loan.getEndDate())) {
            throw new IllegalArgumentException("La cobertura debe ir al menos del " + loan.getStartDate()
                    + " al " + loan.getEndDate() + " (todo el préstamo, transporte incluido).");
        }

        String type = UploadValidator.requireType(file, Set.of(UploadValidator.PDF));
        InsuranceCertificate cert = InsuranceCertificate.builder()
                .loanRequest(loan)
                .coverageType(data.getCoverageType())
                .insurer(data.getInsurer().trim())
                .policyNumber(data.getPolicyNumber().trim())
                .insuredAmount(data.getInsuredAmount())
                .coverageStart(data.getCoverageStart())
                .coverageEnd(data.getCoverageEnd())
                .document(pdf(file, type, "certificado-seguro.pdf"))
                .uploadedBy(userRepository.getReferenceById(currentUser.currentId()))
                // La póliza del propio prestador no necesita su aprobación.
                .status(byCollector ? ReviewStatus.APPROVED : ReviewStatus.PENDING)
                .reviewedAt(byCollector ? LocalDateTime.now() : null)
                .build();
        insuranceRepository.save(cert);

        loanEventService.recordImportant(loan, (byCollector ? "El coleccionista" : "La fundación")
                + " sube el certificado de seguro (" + coverageLabel(cert.getCoverageType()) + ", "
                + cert.getInsurer() + ", " + euros(cert.getInsuredAmount()) + ")"
                + (byCollector ? "." : ". Pendiente de aprobación del coleccionista."));
        return buildStatus(loan);
    }

    @Transactional
    public LoanDocumentsResponse approveInsurance(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        InsuranceCertificate cert = pendingCertificateForReview(loan);
        cert.setStatus(ReviewStatus.APPROVED);
        cert.setReviewNote(null);
        cert.setReviewedAt(LocalDateTime.now());
        insuranceRepository.save(cert);
        loanEventService.record(loan, "El coleccionista aprueba el certificado de seguro.");
        return buildStatus(loan);
    }

    @Transactional
    public LoanDocumentsResponse rejectInsurance(Long loanId, String reason) {
        LoanRequest loan = findOrThrow(loanId);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Indica el motivo para que la fundación pueda corregirlo.");
        }
        InsuranceCertificate cert = pendingCertificateForReview(loan);
        cert.setStatus(ReviewStatus.REJECTED);
        cert.setReviewNote(reason.trim());
        cert.setReviewedAt(LocalDateTime.now());
        insuranceRepository.save(cert);
        loanEventService.recordImportant(loan, "El coleccionista rechaza el certificado de seguro: " + reason.trim());
        return buildStatus(loan);
    }

    // ===== Permiso de exportación =====

    @Transactional
    public LoanDocumentsResponse uploadPermit(Long loanId, ExportPermitRequest data,
                                              MultipartFile file) throws IOException {
        LoanRequest loan = findOrThrow(loanId);
        requireDocumentPhase(loan);
        currentUser.requireAnyUserId(loan.getArtwork().getCollector().getId(), loan.getFoundation().getId());

        if (data.getIssueDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la resolución no puede ser futura.");
        }
        if (data.getReturnDeadline().isBefore(loan.getEndDate())) {
            throw new IllegalArgumentException("La resolución exige el retorno antes del fin del préstamo ("
                    + loan.getEndDate() + "). Ajusta las fechas del préstamo o solicita una prórroga.");
        }

        String type = UploadValidator.requireType(file, Set.of(UploadValidator.PDF));
        ExportPermit permit = permitRepository.save(ExportPermit.builder()
                .loanRequest(loan)
                .resolutionNumber(data.getResolutionNumber().trim())
                .issuedBy(data.getIssuedBy().trim())
                .issueDate(data.getIssueDate())
                .returnDeadline(data.getReturnDeadline())
                .document(pdf(file, type, "permiso-exportacion.pdf"))
                .uploadedBy(userRepository.getReferenceById(currentUser.currentId()))
                .build());

        loanEventService.record(loan, "Se adjunta el permiso de exportación temporal ("
                + permit.getResolutionNumber() + "). La obra debe volver antes del "
                + permit.getReturnDeadline() + ".");
        return buildStatus(loan);
    }

    /**
     * Comprueba si el préstamo puede ampliarse hasta {@code newEnd}. Lanza
     * una excepción si el permiso de exportación no lo permite (seguir
     * fuera sería exportación ilícita) y devuelve avisos no bloqueantes,
     * como un seguro que habrá que ampliar.
     */
    @Transactional(readOnly = true)
    public List<String> extensionWarnings(LoanRequest loan, LocalDate newEnd) {
        if (permitRequirement(loan).required()) {
            ExportPermit permit = permitRepository
                    .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
            if (permit != null && permit.getReturnDeadline().isBefore(newEnd)) {
                throw new IllegalStateException("El permiso de exportación exige el retorno antes del "
                        + permit.getReturnDeadline() + ". Pedid una prórroga al Ministerio de Cultura y "
                        + "adjuntad la nueva resolución antes de ampliar.");
            }
        }
        List<String> warnings = new ArrayList<>();
        insuranceRepository.findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId())
                .filter(c -> c.getCoverageEnd().isBefore(newEnd))
                .ifPresent(c -> warnings.add("El seguro cubre hasta el " + c.getCoverageEnd()
                        + ": la institución debe ampliarlo hasta el " + newEnd + "."));
        return warnings;
    }

    /** Si la obra necesita permiso para este préstamo, y por qué. */
    public record PermitRequirement(boolean required, String reason) {
    }

    public PermitRequirement permitRequirement(LoanRequest loan) {
        Exhibition ex = loan.getExhibition();
        if (ex == null) {
            return new PermitRequirement(false, "Préstamo sin exposición asociada: no se puede determinar el destino.");
        }
        // En una exposición itinerante basta con que una sede esté fuera de España.
        if (ex.tourCountries().stream().allMatch("ES"::equals)) {
            return new PermitRequirement(false, "La obra no sale de España.");
        }
        Artwork a = loan.getArtwork();
        if (a.getHeritageStatus() == Artwork.HeritageStatus.BIC) {
            return new PermitRequirement(true, "Es Bien de Interés Cultural y sale de España.");
        }
        if (a.getHeritageStatus() == Artwork.HeritageStatus.INVENTORIED) {
            return new PermitRequirement(true, "Está en el Inventario General y sale de España.");
        }
        if (a.getYear() == null) {
            return new PermitRequirement(true,
                    "Sale de España y no consta su año: si tiene 100 años o más necesita permiso.");
        }
        int age = LocalDate.now().getYear() - a.getYear();
        if (age >= HERITAGE_AGE_YEARS) {
            return new PermitRequirement(true, "Tiene " + age + " años y sale de España.");
        }
        return new PermitRequirement(false, "Sale de España, pero tiene menos de 100 años y no está protegida.");
    }

    // ===== Helpers =====

    private LoanRequest findOrThrow(Long id) {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", id));
    }

    private void requireDocumentPhase(LoanRequest loan) {
        if (!DOCUMENT_STATUSES.contains(loan.getStatus())) {
            throw new IllegalStateException(
                    "La documentación se gestiona entre la aceptación del préstamo y la recogida de la obra.");
        }
    }

    private InsuranceCertificate pendingCertificateForReview(LoanRequest loan) {
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        InsuranceCertificate cert = insuranceRepository
                .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId())
                .orElseThrow(() -> new IllegalStateException("No hay ningún certificado de seguro que revisar."));
        if (cert.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("El certificado ya está revisado.");
        }
        return cert;
    }

    private static double agreedValue(LoanRequest loan) {
        Double v = loan.getAgreedValue() != null ? loan.getAgreedValue() : loan.getArtwork().getEstimatedValue();
        return v != null ? v : 0;
    }

    private List<String> missing(LoanRequest loan) {
        List<String> missing = new ArrayList<>();
        InsuranceCertificate cert = insuranceRepository
                .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
        if (cert == null) {
            missing.add("Falta el certificado de seguro.");
        } else if (cert.getStatus() == ReviewStatus.PENDING) {
            missing.add("El certificado de seguro está pendiente de aprobación del coleccionista.");
        } else if (cert.getStatus() == ReviewStatus.REJECTED) {
            missing.add("El certificado de seguro fue rechazado; hay que subir uno nuevo.");
        }

        if (permitRequirement(loan).required()) {
            ExportPermit permit = permitRepository
                    .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
            if (permit == null) {
                missing.add("Falta el permiso de exportación temporal.");
            } else if (permit.getReturnDeadline().isBefore(loan.getEndDate())) {
                missing.add("El permiso de exportación exige el retorno antes del fin del préstamo.");
            }
        }
        return missing;
    }

    private LoanDocumentsResponse buildStatus(LoanRequest loan) {
        InsuranceCertificate cert = insuranceRepository
                .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
        ExportPermit permit = permitRepository
                .findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(loan.getId()).orElse(null);
        PermitRequirement req = permitRequirement(loan);
        List<String> missing = missing(loan);

        return LoanDocumentsResponse.builder()
                .agreedValue(agreedValue(loan))
                .insurance(cert == null ? null : LoanDocumentsResponse.Insurance.builder()
                        .id(cert.getId())
                        .coverageType(cert.getCoverageType().name())
                        .insurer(cert.getInsurer())
                        .policyNumber(cert.getPolicyNumber())
                        .insuredAmount(cert.getInsuredAmount())
                        .coverageStart(cert.getCoverageStart())
                        .coverageEnd(cert.getCoverageEnd())
                        .documentFileId(cert.getDocument().getId())
                        .documentFileName(cert.getDocument().getFileName())
                        .status(cert.getStatus().name())
                        .reviewNote(cert.getReviewNote())
                        .uploadedByName(cert.getUploadedBy().getName())
                        .createdAt(cert.getCreatedAt())
                        .reviewedAt(cert.getReviewedAt())
                        .build())
                .permitRequired(req.required())
                .permitReason(req.reason())
                .permit(permit == null ? null : LoanDocumentsResponse.Permit.builder()
                        .id(permit.getId())
                        .resolutionNumber(permit.getResolutionNumber())
                        .issuedBy(permit.getIssuedBy())
                        .issueDate(permit.getIssueDate())
                        .returnDeadline(permit.getReturnDeadline())
                        .documentFileId(permit.getDocument().getId())
                        .documentFileName(permit.getDocument().getFileName())
                        .uploadedByName(permit.getUploadedBy().getName())
                        .createdAt(permit.getCreatedAt())
                        .build())
                .readyForShipment(missing.isEmpty())
                .missing(missing)
                .build();
    }

    private static DBFile pdf(MultipartFile file, String type, String fallbackName) throws IOException {
        return DBFile.builder()
                .fileName(UploadValidator.safeFileName(file.getOriginalFilename(), fallbackName))
                .fileType(type)
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();
    }

    private static String coverageLabel(CoverageType t) {
        return switch (t) {
            case BORROWER_POLICY -> "póliza de la institución";
            case LENDER_POLICY -> "póliza del prestador";
            case STATE_INDEMNITY -> "Garantía del Estado";
        };
    }

    private static String euros(double amount) {
        return String.format(Locale.forLanguageTag("es-ES"), "%,.0f €", amount);
    }
}
