package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ReviewDtos.*;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.Review;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ReviewRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Valoraciones entre las partes de un préstamo ya cerrado (devuelto).
 * Cada parte puede valorar una vez a cada una de las demás.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public LoanReviews forLoan(Long loanId) {
        LoanRequest loan = findOrThrow(loanId);
        Map<Long, User> parties = notificationService.parties(loan);
        Long me = currentUser.currentId();
        if (!parties.containsKey(me) && !currentUser.isAdmin()) {
            throw new AccessDeniedException("No participas en este préstamo");
        }
        List<ReviewItem> reviews = reviewRepository.findByLoanRequestIdOrderByCreatedAtAsc(loanId).stream()
                .map(r -> new ReviewItem(r.getId(), r.getReviewer().getName(), r.getReviewer().getRole().name(),
                        r.getReviewee().getName(), r.getReviewee().getRole().name(),
                        r.getRating(), r.getComment(), r.getCreatedAt()))
                .toList();

        boolean closed = loan.getStatus() == LoanRequest.Status.RETURNED;
        List<Reviewable> pending = !closed || !parties.containsKey(me) ? List.of()
                : parties.values().stream()
                        .filter(u -> !u.getId().equals(me))
                        .filter(u -> !reviewRepository.existsByLoanRequestIdAndReviewerIdAndRevieweeId(loanId, me, u.getId()))
                        .map(u -> new Reviewable(u.getId(), u.getName(), u.getRole().name()))
                        .toList();
        return new LoanReviews(reviews, pending, closed && parties.containsKey(me));
    }

    @Transactional
    public LoanReviews create(Long loanId, CreateReview req) {
        LoanRequest loan = findOrThrow(loanId);
        if (loan.getStatus() != LoanRequest.Status.RETURNED) {
            throw new IllegalStateException("Se puede valorar cuando la obra ha vuelto y el préstamo está cerrado.");
        }
        Map<Long, User> parties = notificationService.parties(loan);
        Long me = currentUser.currentId();
        if (!parties.containsKey(me)) {
            throw new AccessDeniedException("No participas en este préstamo");
        }
        User reviewee = parties.get(req.revieweeId());
        if (reviewee == null || reviewee.getId().equals(me)) {
            throw new IllegalArgumentException("Solo puedes valorar a las otras partes del préstamo.");
        }
        if (reviewRepository.existsByLoanRequestIdAndReviewerIdAndRevieweeId(loanId, me, reviewee.getId())) {
            throw new IllegalStateException("Ya has valorado a esta parte en este préstamo.");
        }
        reviewRepository.save(Review.builder()
                .loanRequest(loan)
                .reviewer(parties.get(me))
                .reviewee(reviewee)
                .rating(req.rating())
                .comment(req.comment() != null && !req.comment().isBlank() ? req.comment().trim() : null)
                .build());
        notificationService.notify(reviewee, "Nueva valoración",
                "Has recibido una valoración de " + req.rating() + "/5 por el préstamo de «"
                        + loan.getArtwork().getTitle() + "».", "/app/loans/" + loanId, false);
        return forLoan(loanId);
    }

    /** Resumen público: media, número y los cinco últimos comentarios sin autor. */
    @Transactional(readOnly = true)
    public RatingSummary summary(Long userId) {
        List<Review> all = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
        OptionalDouble avg = all.stream().mapToInt(Review::getRating).average();
        List<PublicComment> recent = all.stream()
                .filter(r -> r.getComment() != null)
                .limit(5)
                .map(r -> new PublicComment(r.getRating(), r.getComment(), r.getCreatedAt()))
                .toList();
        return new RatingSummary(avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null,
                all.size(), recent);
    }

    private LoanRequest findOrThrow(Long id) {
        return loanRequestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Préstamo", id));
    }
}
