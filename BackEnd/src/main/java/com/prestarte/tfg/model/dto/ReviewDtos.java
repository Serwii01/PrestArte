package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** DTOs de las valoraciones entre partes. */
public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** Nueva valoración de una de las otras partes del préstamo. */
    public record CreateReview(
            @NotNull(message = "Indica a quién valoras") Long revieweeId,
            @NotNull(message = "Indica una puntuación") @Min(value = 1, message = "Mínimo 1")
            @Max(value = 5, message = "Máximo 5") Integer rating,
            @Size(max = 1000) String comment) {
    }

    public record ReviewItem(Long id, String reviewerName, String reviewerRole,
                             String revieweeName, String revieweeRole,
                             int rating, String comment, LocalDateTime createdAt) {
    }

    /** Parte a la que el usuario con sesión todavía puede valorar. */
    public record Reviewable(Long userId, String name, String role) {
    }

    /** Valoraciones de un préstamo y a quién falta por valorar. */
    public record LoanReviews(List<ReviewItem> reviews, List<Reviewable> pending, boolean canReview) {
    }

    /** Resumen público de un usuario: media, número y últimos comentarios (sin autor). */
    public record RatingSummary(Double average, long count, List<PublicComment> recent) {
    }

    public record PublicComment(int rating, String comment, LocalDateTime createdAt) {
    }
}
