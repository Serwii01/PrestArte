package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Acceso a la tabla de solicitudes de préstamo.
 *
 * Incluye consultas auxiliares para filtrar por fundación o coleccionista
 * y consultas que detectan solapamientos de fechas para una misma obra,
 * evitando que se comprometa una obra con dos préstamos coincidentes en
 * el tiempo.
 */
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    /**
     * Estados en los que el préstamo ya compromete la obra: desde que el
     * coleccionista acepta hasta que la obra vuelve a casa.
     */
    Collection<LoanRequest.Status> COMMITTED_STATUSES = EnumSet.of(
            LoanRequest.Status.ACCEPTED,
            LoanRequest.Status.QUOTE_PENDING,
            LoanRequest.Status.QUOTE_PROPOSED,
            LoanRequest.Status.PAID,
            LoanRequest.Status.READY_FOR_PICKUP,
            LoanRequest.Status.IN_TRANSIT,
            LoanRequest.Status.DELIVERED,
            LoanRequest.Status.ON_LOAN,
            LoanRequest.Status.RETURNING);

    /** Devuelve los préstamos de una fundación filtrados por estado. */
    List<LoanRequest> findByFoundationIdAndStatus(Long foundationId, LoanRequest.Status status);

    /** Devuelve todos los préstamos solicitados por una fundación. */
    List<LoanRequest> findByFoundationId(Long foundationId);

    /** Devuelve todos los préstamos asociados a obras de un coleccionista. */
    List<LoanRequest> findByArtworkCollectorId(Long collectorId);

    /** Indica si una fundación ha pedido alguna vez una obra concreta. */
    boolean existsByArtworkIdAndFoundationId(Long artworkId, Long foundationId);

    /** Préstamos de una obra (para su historial de exposiciones). */
    List<LoanRequest> findByArtworkId(Long artworkId);

    /** Solicitudes de una exposición. */
    List<LoanRequest> findByExhibitionId(Long exhibitionId);

    /** Indica si una fundación ha pedido alguna obra a un coleccionista. */
    boolean existsByFoundationIdAndArtworkCollectorId(Long foundationId, Long collectorId);

    /** Indica si un coleccionista tiene alguna solicitud dentro de una exposición. */
    boolean existsByExhibitionIdAndArtworkCollectorId(Long exhibitionId, Long collectorId);

    /**
     * Préstamos de una obra, en alguno de los estados indicados, cuyas
     * fechas se solapan con el intervalo dado. Excluye el préstamo
     * {@code excludeId} (el que se está evaluando).
     */
    @Query("SELECT l FROM LoanRequest l " +
            "WHERE l.artwork.id = :artworkId " +
            "AND l.id <> :excludeId " +
            "AND l.status IN :statuses " +
            "AND (:startDate < l.endDate AND :endDate > l.startDate)")
    List<LoanRequest> findOverlapping(@Param("artworkId") Long artworkId,
                                      @Param("excludeId") Long excludeId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("statuses") Collection<LoanRequest.Status> statuses);

    /**
     * Indica si la obra ya está comprometida con otro préstamo en fechas
     * que se solapan con el intervalo indicado.
     */
    default boolean existsOverlappingLoan(Long artworkId, Long excludeId,
                                          LocalDate startDate, LocalDate endDate) {
        return !findOverlapping(artworkId, excludeId == null ? -1L : excludeId,
                startDate, endDate, COMMITTED_STATUSES).isEmpty();
    }
}
