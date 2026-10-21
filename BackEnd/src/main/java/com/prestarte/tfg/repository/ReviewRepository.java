package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a las valoraciones entre las partes de los préstamos. */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"reviewer", "reviewee"})
    List<Review> findByLoanRequestIdOrderByCreatedAtAsc(Long loanRequestId);

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    /** Valoraciones escritas por un usuario (para la exportación de sus datos). */
    List<Review> findByReviewerIdOrderByCreatedAtDesc(Long reviewerId);

    boolean existsByLoanRequestIdAndReviewerIdAndRevieweeId(Long loanRequestId, Long reviewerId, Long revieweeId);
}
