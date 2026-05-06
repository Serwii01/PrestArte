package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    // Este es el que te está dando el error:
    List<LoanRequest> findByFoundationIdAndStatus(Long foundationId, LoanRequest.Status status);

    List<LoanRequest> findByFoundationId(Long foundationId);

    List<LoanRequest> findByArtworkCollectorId(Long collectorId);

    // El método de validación de fechas que hicimos antes
    @Query("SELECT COUNT(l) > 0 FROM LoanRequest l " +
            "WHERE l.artwork.id = :artworkId " +
            "AND l.status = 'ACCEPTED' " +
            "AND (:startDate < l.endDate AND :endDate > l.startDate)")
    boolean existsOverlappingLoan(@Param("artworkId") Long artworkId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}