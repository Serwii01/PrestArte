package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ConditionReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a los informes de estado de las obras. */
@Repository
public interface ConditionReportRepository extends JpaRepository<ConditionReport, Long> {

    /** Informes de todos los envíos de un préstamo, en orden cronológico. */
    @EntityGraph(attributePaths = {"photos", "author", "shipment"})
    List<ConditionReport> findByShipmentLoanRequestIdOrderByCreatedAtAscIdAsc(Long loanRequestId);

    /** Busca el informe al que pertenece una fotografía, para comprobar quién puede verla. */
    @Query("SELECT r FROM ConditionReport r JOIN r.photos p WHERE p.id = :fileId")
    Optional<ConditionReport> findByPhotoId(@Param("fileId") String fileId);
}
