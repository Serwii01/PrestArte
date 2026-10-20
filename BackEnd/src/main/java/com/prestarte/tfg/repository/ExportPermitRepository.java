package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ExportPermit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a los permisos de exportación temporal de los préstamos. */
@Repository
public interface ExportPermitRepository extends JpaRepository<ExportPermit, Long> {

    /** Permiso vigente (el más reciente) de un préstamo. */
    Optional<ExportPermit> findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(Long loanRequestId);

    Optional<ExportPermit> findByDocumentId(String fileId);
}
