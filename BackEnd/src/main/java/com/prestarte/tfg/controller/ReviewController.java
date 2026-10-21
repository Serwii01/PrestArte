package com.prestarte.tfg.controller;

import com.prestarte.tfg.model.dto.ReviewDtos.CreateReview;
import com.prestarte.tfg.model.dto.ReviewDtos.LoanReviews;
import com.prestarte.tfg.model.dto.ReviewDtos.RatingSummary;
import com.prestarte.tfg.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Valoraciones entre las partes de los préstamos. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/loan-requests/{id}/reviews")
    public ResponseEntity<LoanReviews> forLoan(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.forLoan(id));
    }

    @PostMapping("/loan-requests/{id}/reviews")
    public ResponseEntity<LoanReviews> create(@PathVariable Long id, @Valid @RequestBody CreateReview body) {
        return ResponseEntity.ok(reviewService.create(id, body));
    }

    /** Resumen público de valoraciones de un usuario (p. ej. un transportista). */
    @GetMapping("/users/{id}/rating")
    public ResponseEntity<RatingSummary> rating(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.summary(id));
    }
}
