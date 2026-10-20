package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.CompatibilityReport;
import com.prestarte.tfg.model.dto.FacilityReportRequest;
import com.prestarte.tfg.model.dto.FacilityReportResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.FacilityReportRepository;
import com.prestarte.tfg.repository.FoundationRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Gestiona el informe de instalaciones de cada fundación y la
 * comprobación de compatibilidad con los requisitos de las obras.
 *
 * Quién ve el informe: la propia fundación, la administración y los
 * coleccionistas que han recibido alguna solicitud de esa fundación
 * (son quienes lo necesitan para decidir). Al resto no se les muestra,
 * porque describe la seguridad del edificio.
 */
@Service
@RequiredArgsConstructor
public class FacilityReportService {

    private final FacilityReportRepository facilityReportRepository;
    private final FoundationRepository foundationRepository;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final CompatibilityService compatibilityService;
    private final CurrentUser currentUser;

    /** Informe de la fundación con sesión, si lo tiene. */
    @Transactional(readOnly = true)
    public Optional<FacilityReportResponse> getMine() {
        Long foundationId = requireFoundation();
        return facilityReportRepository.findByFoundationId(foundationId).map(this::toResponse);
    }

    /** Crea o actualiza el informe de la fundación con sesión. */
    @Transactional
    public FacilityReportResponse saveMine(FacilityReportRequest req) {
        Long foundationId = requireFoundation();
        validateRange(req.getTempMin(), req.getTempMax(), "temperatura");
        validateRange(req.getRhMin(), req.getRhMax(), "humedad");

        FacilityReport report = facilityReportRepository.findByFoundationId(foundationId)
                .orElseGet(() -> FacilityReport.builder()
                        .foundation(foundationRepository.getReferenceById(foundationId))
                        .build());

        report.setClimateControlled(req.getClimateControlled());
        report.setTempMin(req.getTempMin());
        report.setTempMax(req.getTempMax());
        report.setRhMin(req.getRhMin());
        report.setRhMax(req.getRhMax());
        report.setMinAchievableLux(req.getMinAchievableLux());
        report.setUvFiltered(req.getUvFiltered());
        report.setGuards24h(req.getGuards24h());
        report.setCctv(req.getCctv());
        report.setIntrusionAlarm(req.getIntrusionAlarm());
        report.setFireDetection(req.getFireDetection());
        report.setFireSuppression(blankToNull(req.getFireSuppression()));
        report.setDisplayCasesAvailable(req.getDisplayCasesAvailable());
        report.setAccessWidthCm(req.getAccessWidthCm());
        report.setAccessHeightCm(req.getAccessHeightCm());
        report.setLoadingDock(req.getLoadingDock());
        report.setNotes(blankToNull(req.getNotes()));

        return toResponse(facilityReportRepository.saveAndFlush(report));
    }

    /** Adjunta (o sustituye) el PDF oficial del informe. Exige haber guardado antes el informe. */
    @Transactional
    public FacilityReportResponse uploadDocument(MultipartFile file) throws IOException {
        Long foundationId = requireFoundation();
        FacilityReport report = facilityReportRepository.findByFoundationId(foundationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Guarda primero el informe de instalaciones y después adjunta el PDF."));
        String type = UploadValidator.requireType(file, java.util.Set.of(UploadValidator.PDF));
        report.setDocument(DBFile.builder()
                .fileName(UploadValidator.safeFileName(file.getOriginalFilename(), "informe-instalaciones.pdf"))
                .fileType(type)
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build());
        return toResponse(facilityReportRepository.saveAndFlush(report));
    }

    /** Informe de una fundación concreta, si la sesión actual puede verlo. */
    @Transactional(readOnly = true)
    public FacilityReportResponse getForFoundation(Long foundationId) {
        requireReadAccess(foundationId);
        return facilityReportRepository.findByFoundationId(foundationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Esta institución todavía no ha completado su informe de instalaciones."));
    }

    /**
     * Compatibilidad de una obra con las instalaciones de la fundación con
     * sesión. Se usa antes de enviar la solicitud; {@code startDate}
     * permite comprobar también la antelación mínima.
     */
    @Transactional(readOnly = true)
    public CompatibilityReport checkArtworkForMe(Long artworkId, LocalDate startDate) {
        Long foundationId = requireFoundation();
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        FacilityReport report = facilityReportRepository.findByFoundationId(foundationId).orElse(null);
        return compatibilityService.check(artwork, report, startDate);
    }

    /** Compatibilidad de la obra de un préstamo con la fundación que lo pide. */
    @Transactional(readOnly = true)
    public CompatibilityReport checkLoan(LoanRequest loan) {
        FacilityReport report = facilityReportRepository
                .findByFoundationId(loan.getFoundation().getId()).orElse(null);
        return compatibilityService.check(loan.getArtwork(), report, null);
    }

    /**
     * Comprueba si la sesión actual puede ver el informe (o su PDF) de la
     * fundación indicada. También lo usa {@link FileStorageService}.
     */
    public void requireReadAccess(Long foundationId) {
        if (currentUser.isAdmin()) return;
        Long me = currentUser.currentId();
        if (me.equals(foundationId)) return;
        if (loanRequestRepository.existsByFoundationIdAndArtworkCollectorId(foundationId, me)) return;
        throw new AccessDeniedException(
                "Solo los coleccionistas que han recibido una solicitud de esta institución pueden ver su informe");
    }

    // ===== Helpers =====

    private Long requireFoundation() {
        User me = currentUser.getOrThrow();
        if (me.getRole() != Role.FOUNDATION) {
            throw new AccessDeniedException("Solo las fundaciones tienen informe de instalaciones");
        }
        return me.getId();
    }

    private static void validateRange(Double min, Double max, String what) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("El mínimo de " + what + " no puede ser mayor que el máximo.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private FacilityReportResponse toResponse(FacilityReport r) {
        DBFile doc = r.getDocument();
        return FacilityReportResponse.builder()
                .foundationId(r.getFoundation().getId())
                .foundationName(r.getFoundation().getName())
                .climateControlled(r.getClimateControlled())
                .tempMin(r.getTempMin())
                .tempMax(r.getTempMax())
                .rhMin(r.getRhMin())
                .rhMax(r.getRhMax())
                .minAchievableLux(r.getMinAchievableLux())
                .uvFiltered(r.getUvFiltered())
                .guards24h(r.getGuards24h())
                .cctv(r.getCctv())
                .intrusionAlarm(r.getIntrusionAlarm())
                .fireDetection(r.getFireDetection())
                .fireSuppression(r.getFireSuppression())
                .displayCasesAvailable(r.getDisplayCasesAvailable())
                .accessWidthCm(r.getAccessWidthCm())
                .accessHeightCm(r.getAccessHeightCm())
                .loadingDock(r.getLoadingDock())
                .notes(r.getNotes())
                .documentFileId(doc != null ? doc.getId() : null)
                .documentFileName(doc != null ? doc.getFileName() : null)
                .updatedAt(r.getUpdatedAt())
                .current(r.isCurrent())
                .build();
    }
}
