package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    // Solicitudes hechas por una fundación
    List<LoanRequest> findByFoundationId(Long foundationId);

    // Solicitudes recibidas por un coleccionista (mirando el dueño de la obra)
    List<LoanRequest> findByArtworkCollectorId(Long collectorId);

    // Filtrar por estado (PENDIENTE, APROBADO, etc.)
    List<LoanRequest> findByStatus(LoanRequest.Status status);
}