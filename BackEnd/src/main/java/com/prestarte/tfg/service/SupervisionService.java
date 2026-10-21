package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.SupervisionCheckpointRequest;
import com.prestarte.tfg.model.dto.SupervisionCheckpointResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.model.entity.SupervisionCheckpoint.Stage;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.SupervisionCheckpointRepository;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Supervisión remota de un préstamo ("correo virtual").
 *
 * El transportista o la institución documentan con fotos cada fase en la
 * que se manipula la obra y el coleccionista las revisa. Si la obra exige
 * correo virtual ({@link CourierMode#VIRTUAL}), la recogida queda
 * bloqueada hasta que el coleccionista aprueba el embalaje.
 */
@Service
@RequiredArgsConstructor
public class SupervisionService {

    private static final int MAX_PHOTOS = 10;

    private static final Map<Stage, String> STAGE_LABEL = Map.of(
            Stage.PACKING, "embalaje",
            Stage.LOADING, "carga",
            Stage.UNLOADING, "descarga",
            Stage.INSTALLATION, "montaje",
            Stage.DEINSTALLATION, "desmontaje",
            Stage.REPACKING, "embalaje para la devolución");

    /** Estados en los que hay obra que manipular. */
    private static final Set<LoanRequest.Status> ACTIVE = EnumSet.of(
            LoanRequest.Status.PAID,
            LoanRequest.Status.READY_FOR_PICKUP,
            LoanRequest.Status.IN_TRANSIT,
            LoanRequest.Status.DELIVERED,
            LoanRequest.Status.ON_LOAN,
            LoanRequest.Status.RETURNING);

    private final SupervisionCheckpointRepository checkpointRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;
    private final LoanEventService loanEventService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<SupervisionCheckpointResponse> list(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        chatSessionService.requireParticipant(loan);
        return checkpointRepository.findByLoanRequestIdOrderByCreatedAtAscIdAsc(loanId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** El transportista o la institución documentan una fase con fotos. */
    @Transactional
    public SupervisionCheckpointResponse create(Long loanId, SupervisionCheckpointRequest req,
                                                List<MultipartFile> photos) throws IOException {
        LoanRequest loan = findOrThrow(loanId);
        chatSessionService.requireParticipant(loan);
        if (currentUser.isAnyOf(loan.getArtwork().getCollector().getId())) {
            throw new AccessDeniedException("Las fases las documenta quien manipula la obra; tú las revisas.");
        }
        if (!ACTIVE.contains(loan.getStatus())) {
            throw new IllegalStateException("La supervisión empieza cuando el presupuesto está aprobado.");
        }

        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Adjunta al menos una fotografía.");
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

        SupervisionCheckpoint c = checkpointRepository.save(SupervisionCheckpoint.builder()
                .loanRequest(loan)
                .stage(req.getStage())
                .note(req.getNote() != null && !req.getNote().isBlank() ? req.getNote().trim() : null)
                .photos(dbFiles)
                .author(userRepository.getReferenceById(currentUser.currentId()))
                .build());

        loanEventService.recordImportant(loan, "Se documenta el " + STAGE_LABEL.get(c.getStage())
                + " con " + dbFiles.size() + " foto(s) para la supervisión del coleccionista.");
        return toResponse(c);
    }

    @Transactional
    public SupervisionCheckpointResponse approve(Long loanId, Long checkpointId) {
        SupervisionCheckpoint c = pendingForReview(loanId, checkpointId);
        c.setStatus(SupervisionCheckpoint.Status.APPROVED);
        c.setReviewNote(null);
        c.setReviewedAt(LocalDateTime.now());
        loanEventService.record(c.getLoanRequest(), "El coleccionista da el visto bueno al "
                + STAGE_LABEL.get(c.getStage()) + ".");
        return toResponse(checkpointRepository.save(c));
    }

    @Transactional
    public SupervisionCheckpointResponse requestChanges(Long loanId, Long checkpointId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Indica qué hay que corregir.");
        }
        SupervisionCheckpoint c = pendingForReview(loanId, checkpointId);
        c.setStatus(SupervisionCheckpoint.Status.CHANGES_REQUESTED);
        c.setReviewNote(reason.trim());
        c.setReviewedAt(LocalDateTime.now());
        loanEventService.recordImportant(c.getLoanRequest(), "El coleccionista pide cambios en el "
                + STAGE_LABEL.get(c.getStage()) + ": " + reason.trim());
        return toResponse(checkpointRepository.save(c));
    }

    /**
     * Si la obra exige correo virtual, comprueba que el coleccionista ha
     * aprobado el embalaje correspondiente antes de la recogida.
     */
    @Transactional(readOnly = true)
    public void requireApprovedPacking(LoanRequest loan, boolean isReturn) {
        if (courierMode(loan) != CourierMode.VIRTUAL) return;
        Stage stage = isReturn ? Stage.REPACKING : Stage.PACKING;
        boolean ok = checkpointRepository.existsByLoanRequestIdAndStageAndStatus(
                loan.getId(), stage, SupervisionCheckpoint.Status.APPROVED);
        if (!ok) {
            throw new IllegalStateException("Esta obra exige correo virtual: el coleccionista debe aprobar el "
                    + STAGE_LABEL.get(stage) + " (con fotos en «Supervisión») antes de la recogida.");
        }
    }

    public static CourierMode courierMode(LoanRequest loan) {
        LoanRequirements req = loan.getArtwork().getRequirements();
        return req != null && req.getCourierMode() != null ? req.getCourierMode() : CourierMode.NONE;
    }

    // ===== Helpers =====

    private LoanRequest findOrThrow(Long id) {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Préstamo", id));
    }

    private SupervisionCheckpoint pendingForReview(Long loanId, Long checkpointId) {
        SupervisionCheckpoint c = checkpointRepository.findById(checkpointId)
                .orElseThrow(() -> ResourceNotFoundException.of("Hito de supervisión", checkpointId));
        LoanRequest loan = c.getLoanRequest();
        if (!loan.getId().equals(loanId)) {
            throw new AccessDeniedException("El hito no pertenece a este préstamo.");
        }
        currentUser.requireUserId(loan.getArtwork().getCollector().getId());
        if (c.getStatus() != SupervisionCheckpoint.Status.PENDING) {
            throw new IllegalStateException("Este hito ya está revisado.");
        }
        return c;
    }

    private SupervisionCheckpointResponse toResponse(SupervisionCheckpoint c) {
        return SupervisionCheckpointResponse.builder()
                .id(c.getId())
                .stage(c.getStage().name())
                .note(c.getNote())
                .photoIds(c.getPhotos().stream().map(DBFile::getId).toList())
                .authorName(c.getAuthor().getName())
                .authorRole(c.getAuthor().getRole().name())
                .createdAt(c.getCreatedAt())
                .status(c.getStatus().name())
                .reviewNote(c.getReviewNote())
                .reviewedAt(c.getReviewedAt())
                .build();
    }
}
