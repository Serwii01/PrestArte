package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.dto.LoanResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final ArtworkRepository artworkRepository;
    private final FoundationRepository foundationRepository;

    @Transactional
    public LoanResponse createRequest(CreateLoanRequest dto) {
        Artwork artwork = artworkRepository.findById(dto.getArtworkId())
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        Foundation foundation = foundationRepository.findById(dto.getFoundationId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        LoanRequest request = LoanRequest.builder()
                .artwork(artwork)
                .foundation(foundation)
                .startDate(dto.getStartDate()) // <--- Actualizado
                .endDate(dto.getEndDate())     // <--- Actualizado
                .status(LoanRequest.Status.PENDIENTE)
                .build();

        LoanRequest saved = loanRequestRepository.save(request);

        return LoanResponse.builder()
                .id(saved.getId())
                .artworkTitle(artwork.getTitle())
                .foundationName(foundation.getName())
                .startDate(saved.getStartDate()) // <--- Actualizado
                .endDate(saved.getEndDate())     // <--- Actualizado
                .status(saved.getStatus().name())
                .build();
    }

    public List<LoanRequest> getRequestsByFoundation(Long foundationId) {
        return loanRequestRepository.findByFoundationId(foundationId);
    }

    public List<LoanRequest> getRequestsByCollector(Long collectorId) {
        // Cambiamos 'Owner' por 'Collector' para que coincida con tu Repository
        return loanRequestRepository.findByArtworkCollectorId(collectorId);
    }

    @Transactional
    public LoanResponse updateStatus(Long loanId, LoanRequest.Status status) {
        LoanRequest request = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Petición no encontrada"));

        request.setStatus(status);
        LoanRequest saved = loanRequestRepository.save(request);

        return LoanResponse.builder()
                .id(saved.getId())
                .artworkTitle(saved.getArtwork().getTitle())
                .foundationName(saved.getFoundation().getName())
                .startDate(saved.getStartDate()) // <--- Actualizado
                .endDate(saved.getEndDate())     // <--- Actualizado
                .status(saved.getStatus().name())
                .build();
    }
}