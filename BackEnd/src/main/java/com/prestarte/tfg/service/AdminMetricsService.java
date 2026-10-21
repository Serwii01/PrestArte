package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Indicadores para la administración: cómo está la red (usuarios, obras,
 * préstamos), cuánto tardan los coleccionistas en responder, qué obras
 * se piden más y cuánto valor asegurado está circulando.
 */
@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    /** Préstamos con la obra ya fuera de casa del prestador. */
    private static final Set<LoanRequest.Status> OUT = EnumSet.of(
            LoanRequest.Status.IN_TRANSIT, LoanRequest.Status.DELIVERED,
            LoanRequest.Status.ON_LOAN, LoanRequest.Status.RETURNING);

    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanEventRepository loanEventRepository;
    private final ConditionReportRepository conditionReportRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final CurrentUser currentUser;

    public record TopArtwork(Long id, String title, String artist, long requests) {
    }

    public record Metrics(
            Map<String, Long> approvedUsersByRole,
            long pendingUsers,
            long deletedUsers,
            long artworks,
            long lendableArtworks,
            long verifiedArtworks,
            long flaggedArtworks,
            long pendingDueDiligence,
            Map<String, Long> loansByStatus,
            long activeLoans,
            long completedLoans,
            /** % de solicitudes respondidas que se aceptaron. */
            Double acceptanceRate,
            /** Días medios entre la solicitud y la respuesta del coleccionista. */
            Double avgDaysToAnswer,
            /** Valor asegurado de las obras que ahora mismo están fuera de casa (€). */
            double insuredValueOut,
            long exhibitions,
            long damageIncidents,
            List<TopArtwork> mostRequested) {
    }

    @Transactional(readOnly = true)
    public Metrics metrics() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo la administración.");
        }
        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = users.stream()
                .filter(u -> u.getDeletedAt() == null && u.getStatus() == UserStatus.APPROVED && u.getRole() != Role.ADMIN)
                .collect(Collectors.groupingBy(u -> u.getRole().name(), TreeMap::new, Collectors.counting()));
        long pendingUsers = users.stream().filter(u -> u.getStatus() == UserStatus.PENDING && u.getDeletedAt() == null).count();
        long deletedUsers = users.stream().filter(u -> u.getDeletedAt() != null).count();

        List<Artwork> artworks = artworkRepository.findAll();
        long lendable = artworks.stream().filter(a -> a.isAvailableForLoan() && a.getTitleDeclaredAt() != null).count();
        Map<Artwork.DueDiligenceStatus, Long> dd = artworks.stream()
                .collect(Collectors.groupingBy(ProvenanceService::status, Collectors.counting()));

        List<LoanRequest> loans = loanRequestRepository.findAll();
        Map<String, Long> byStatus = loans.stream()
                .collect(Collectors.groupingBy(l -> l.getStatus().name(), TreeMap::new, Collectors.counting()));
        long active = loans.stream().filter(l -> LoanRequestRepository.COMMITTED_STATUSES.contains(l.getStatus())).count();
        long completed = byStatus.getOrDefault(LoanRequest.Status.RETURNED.name(), 0L);

        // Tasa de aceptación y tiempo de respuesta, a partir del historial.
        long accepted = 0, answered = 0, totalDays = 0, timed = 0;
        for (LoanRequest l : loans) {
            List<LoanEvent> events = loanEventRepository.findByLoanRequestIdOrderByCreatedAtAscIdAsc(l.getId());
            Optional<LoanEvent> requested = events.stream()
                    .filter(e -> e.getToStatus() == LoanRequest.Status.REQUESTED).findFirst();
            Optional<LoanEvent> answer = events.stream()
                    .filter(e -> e.getToStatus() == LoanRequest.Status.ACCEPTED
                            || (e.getToStatus() == LoanRequest.Status.REJECTED && e.getActor() != null))
                    .findFirst();
            if (answer.isPresent()) {
                answered++;
                if (answer.get().getToStatus() == LoanRequest.Status.ACCEPTED) accepted++;
                if (requested.isPresent()) {
                    totalDays += ChronoUnit.DAYS.between(requested.get().getCreatedAt(), answer.get().getCreatedAt());
                    timed++;
                }
            }
        }

        double insuredOut = loans.stream()
                .filter(l -> OUT.contains(l.getStatus()))
                .mapToDouble(l -> l.getAgreedValue() != null ? l.getAgreedValue()
                        : Optional.ofNullable(l.getArtwork().getEstimatedValue()).orElse(0.0))
                .sum();

        // Se agrupa por id: el hashCode de Lombok en entidades con relaciones
        // bidireccionales (Artwork <-> ArtworkFile) puede entrar en bucle.
        Map<Long, Long> requestsPerArtwork = loans.stream()
                .collect(Collectors.groupingBy(l -> l.getArtwork().getId(), Collectors.counting()));
        Map<Long, Artwork> artworkById = artworks.stream()
                .collect(Collectors.toMap(Artwork::getId, a -> a));
        List<TopArtwork> top = requestsPerArtwork.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .filter(e -> artworkById.containsKey(e.getKey()))
                .map(e -> {
                    Artwork a = artworkById.get(e.getKey());
                    return new TopArtwork(a.getId(), a.getTitle(), a.getArtist(), e.getValue());
                })
                .toList();

        long incidents = conditionReportRepository.findAll().stream().filter(ConditionReport::isDamageReported).count();

        return new Metrics(
                byRole, pendingUsers, deletedUsers,
                artworks.size(), lendable,
                dd.getOrDefault(Artwork.DueDiligenceStatus.CLEAR, 0L),
                dd.getOrDefault(Artwork.DueDiligenceStatus.FLAGGED, 0L),
                dd.getOrDefault(Artwork.DueDiligenceStatus.PENDING, 0L),
                byStatus, active, completed,
                answered > 0 ? Math.round(accepted * 1000.0 / answered) / 10.0 : null,
                timed > 0 ? Math.round(totalDays * 10.0 / timed) / 10.0 : null,
                insuredOut,
                exhibitionRepository.count(),
                incidents,
                top);
    }
}
