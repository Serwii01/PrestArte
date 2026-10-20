package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.InsuranceCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a los certificados de seguro de los préstamos. */
@Repository
public interface InsuranceCertificateRepository extends JpaRepository<InsuranceCertificate, Long> {

    /** Certificado vigente (el más reciente) de un préstamo. */
    Optional<InsuranceCertificate> findFirstByLoanRequestIdOrderByCreatedAtDescIdDesc(Long loanRequestId);

    Optional<InsuranceCertificate> findByDocumentId(String fileId);
}
