package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ExhibitionHistoryItem;
import com.prestarte.tfg.model.dto.ExhibitionRequest;
import com.prestarte.tfg.model.dto.ExhibitionResponse;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Exhibition;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.ExhibitionRepository;
import com.prestarte.tfg.repository.FoundationRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Gestiona las exposiciones de las fundaciones.
 *
 * La fundación organizadora y la administración ven la exposición
 * completa, con todas sus solicitudes. Un coleccionista con alguna
 * solicitud dentro de la exposición ve sus datos (para saber dónde y
 * cuándo se expondría su obra), pero no las obras de los demás.
 */
@Service
@RequiredArgsConstructor
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final FoundationRepository foundationRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    @Transactional
    public ExhibitionResponse create(ExhibitionRequest req) {
        Long foundationId = requireFoundation();
        Exhibition e = Exhibition.builder()
                .foundation(foundationRepository.getReferenceById(foundationId))
                .build();
        apply(e, req);
        return toResponse(exhibitionRepository.save(e), true);
    }

    @Transactional
    public ExhibitionResponse update(Long id, ExhibitionRequest req) {
        Exhibition e = findOrThrow(id);
        requireOrganizer(e);
        apply(e, req);
        return toResponse(exhibitionRepository.save(e), true);
    }

    /** Exposiciones de la fundación con sesión. */
    @Transactional(readOnly = true)
    public List<ExhibitionResponse> listMine() {
        Long foundationId = requireFoundation();
        return exhibitionRepository.findByFoundationIdOrderByOpeningDateAsc(foundationId).stream()
                .map(e -> toResponse(e, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExhibitionResponse get(Long id) {
        Exhibition e = findOrThrow(id);
        if (isOrganizerOrAdmin(e)) return toResponse(e, true);
        Long me = currentUser.currentId();
        if (loanRequestRepository.existsByExhibitionIdAndArtworkCollectorId(id, me)) {
            return toResponse(e, false);
        }
        throw new AccessDeniedException("No tienes acceso a esta exposición");
    }

    /**
     * Devuelve la exposición para asociarle una solicitud, comprobando
     * que pertenece a la fundación que pide la obra.
     */
    @Transactional(readOnly = true)
    public Exhibition getForLoanRequest(Long exhibitionId, Long foundationId) {
        Exhibition e = findOrThrow(exhibitionId);
        if (!e.getFoundation().getId().equals(foundationId)) {
            throw new AccessDeniedException("La exposición no pertenece a tu institución");
        }
        return e;
    }

    /**
     * Historial público de exposiciones de una obra: las exposiciones en
     * las que llegó a estar (entregada, en préstamo o ya devuelta). Haber
     * sido expuesta en museos es parte del valor de una obra.
     */
    @Transactional(readOnly = true)
    public List<ExhibitionHistoryItem> historyOf(Long artworkId) {
        return loanRequestRepository.findByArtworkId(artworkId).stream()
                .filter(l -> l.getExhibition() != null && EXHIBITED.contains(l.getStatus()))
                .filter(l -> ProvenanceService.status(l.getArtwork()) != Artwork.DueDiligenceStatus.FLAGGED)
                .map(LoanRequest::getExhibition)
                .distinct()
                .sorted(Comparator.comparing(Exhibition::getOpeningDate).reversed())
                .map(e -> ExhibitionHistoryItem.builder()
                        .title(e.getTitle())
                        .venueName(e.getVenueName())
                        .venueCity(e.getVenueCity())
                        .foundationName(e.getFoundation().getName())
                        .openingDate(e.getOpeningDate())
                        .closingDate(e.getClosingDate())
                        .build())
                .toList();
    }

    /** Estados en los que la obra llegó a exponerse. */
    static final java.util.Set<LoanRequest.Status> EXHIBITED = java.util.EnumSet.of(
            LoanRequest.Status.DELIVERED, LoanRequest.Status.ON_LOAN,
            LoanRequest.Status.RETURNING, LoanRequest.Status.RETURNED);

    // ===== Helpers =====

    private void apply(Exhibition e, ExhibitionRequest req) {
        if (!req.getClosingDate().isAfter(req.getOpeningDate())) {
            throw new IllegalArgumentException("La fecha de cierre debe ser posterior a la de apertura.");
        }
        e.setTitle(req.getTitle().trim());
        e.setDescription(req.getDescription() != null && !req.getDescription().isBlank()
                ? req.getDescription().trim() : null);
        e.setVenueName(req.getVenueName().trim());
        e.setVenueCity(req.getVenueCity().trim());
        e.setVenueCountry(req.getVenueCountry() != null ? req.getVenueCountry() : "ES");
        e.setOpeningDate(req.getOpeningDate());
        e.setClosingDate(req.getClosingDate());
    }

    private Exhibition findOrThrow(Long id) {
        return exhibitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exposición", id));
    }

    private Long requireFoundation() {
        User me = currentUser.getOrThrow();
        if (me.getRole() != Role.FOUNDATION) {
            throw new AccessDeniedException("Solo las fundaciones pueden gestionar exposiciones");
        }
        return me.getId();
    }

    private boolean isOrganizerOrAdmin(Exhibition e) {
        return currentUser.isAdmin() || currentUser.isAnyOf(e.getFoundation().getId());
    }

    private void requireOrganizer(Exhibition e) {
        if (!isOrganizerOrAdmin(e)) {
            throw new AccessDeniedException("Solo la institución organizadora puede editar la exposición");
        }
    }

    private ExhibitionResponse toResponse(Exhibition e, boolean withLoans) {
        List<ExhibitionResponse.LoanSummary> loans = null;
        if (withLoans && e.getId() != null) {
            loans = loanRequestRepository.findByExhibitionId(e.getId()).stream()
                    .sorted(Comparator.comparing(LoanRequest::getId))
                    .map(l -> ExhibitionResponse.LoanSummary.builder()
                            .loanId(l.getId())
                            .artworkId(l.getArtwork().getId())
                            .artworkTitle(l.getArtwork().getTitle())
                            .artworkArtist(l.getArtwork().getArtist())
                            .collectorName(ownerNameFor(l))
                            .status(l.getStatus().name())
                            .startDate(l.getStartDate())
                            .endDate(l.getEndDate())
                            .build())
                    .toList();
        }
        return ExhibitionResponse.builder()
                .id(e.getId())
                .foundationId(e.getFoundation().getId())
                .foundationName(e.getFoundation().getName())
                .title(e.getTitle())
                .description(e.getDescription())
                .venueName(e.getVenueName())
                .venueCity(e.getVenueCity())
                .venueCountry(e.getVenueCountry() != null ? e.getVenueCountry() : "ES")
                .openingDate(e.getOpeningDate())
                .closingDate(e.getClosingDate())
                .loans(loans)
                .build();
    }

    /**
     * Respeta el anonimato del coleccionista: su nombre solo se muestra
     * si lo ha hecho público o si aceptó el préstamo (existe envío),
     * igual que en {@link LoanRequestService}.
     */
    private String ownerNameFor(LoanRequest l) {
        boolean accepted = shipmentRepository
                .findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
                        l.getId(), Shipment.ShipmentDirection.OUTBOUND)
                .isPresent();
        return l.getArtwork().isShowOwnerName() || accepted || currentUser.isAdminOrNull()
                ? l.getArtwork().getCollector().getName()
                : ArtworkService.ANONYMOUS_OWNER;
    }
}
