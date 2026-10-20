package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.FacilityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a los informes de instalaciones (uno por fundación). */
@Repository
public interface FacilityReportRepository extends JpaRepository<FacilityReport, Long> {

    Optional<FacilityReport> findByFoundationId(Long foundationId);

    /** Busca el informe al que pertenece un PDF adjunto, para comprobar quién puede verlo. */
    Optional<FacilityReport> findByDocumentId(String fileId);
}
