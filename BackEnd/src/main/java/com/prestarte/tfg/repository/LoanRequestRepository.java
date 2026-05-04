package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    // Busca peticiones hechas por una fundación específica
    List<LoanRequest> findByFoundationId(Long foundationId);

    // Busca peticiones recibidas por un coleccionista (dueño de la obra)
    // Usamos 'artworkCollectorId' porque en Artwork el campo es 'collector'
    List<LoanRequest> findByArtworkCollectorId(Long collectorId);

    List<LoanRequest> findByFoundationIdAndStatus(Long foundationId, LoanRequest.Status status);
}