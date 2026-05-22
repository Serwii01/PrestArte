package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a la tabla de solicitudes de préstamo.
 *
 * Incluye consultas auxiliares para filtrar por fundación o coleccionista
 * y un método específico que detecta solapamientos de fechas para una
 * misma obra, evitando que se acepten dos préstamos coincidentes en el
 * tiempo.
 */
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    /** Devuelve los préstamos de una fundación filtrados por estado. */
    List<LoanRequest> findByFoundationIdAndStatus(Long foundationId, LoanRequest.Status status);

    /** Devuelve todos los préstamos solicitados por una fundación. */
    List<LoanRequest> findByFoundationId(Long foundationId);

    /** Devuelve todos los préstamos asociados a obras de un coleccionista. */
    List<LoanRequest> findByArtworkCollectorId(Long collectorId);

    /**
     * Indica si existe algún préstamo aceptado para una obra cuyas fechas
     * solapen con el intervalo indicado. Se utiliza para impedir que el
     * coleccionista comprometa la misma obra a dos fundaciones a la vez.
     */
    @Query("SELECT COUNT(l) > 0 FROM LoanRequest l " +
            "WHERE l.artwork.id = :artworkId " +
            "AND l.status = 'ACCEPTED' " +
            "AND (:startDate < l.endDate AND :endDate > l.startDate)")
    boolean existsOverlappingLoan(@Param("artworkId") Long artworkId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
