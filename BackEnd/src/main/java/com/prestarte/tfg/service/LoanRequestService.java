package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.CreateLoanRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.FoundationRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
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
    public LoanRequest createLoanRequest(CreateLoanRequest request) {
        Artwork artwork = artworkRepository.findById(request.getArtworkId())
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        Foundation foundation = foundationRepository.findById(request.getFoundationId())
                .orElseThrow(() -> new RuntimeException("Fundación no encontrada"));

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setArtwork(artwork);
        loanRequest.setFoundation(foundation);

        // Ahora estos métodos sí coinciden con la Entidad
        loanRequest.setProposedStartDate(request.getProposedStartDate());
        loanRequest.setProposedEndDate(request.getProposedEndDate());

        loanRequest.setAgreedConditions(request.getAdditionalConditions());
        loanRequest.setStatus(LoanRequest.Status.PENDIENTE);

        return loanRequestRepository.save(loanRequest);
    }

    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestRepository.findAll();
    }
    @Transactional
    public LoanRequest updateRequestStatus(Long requestId, LoanRequest.Status newStatus) {
        LoanRequest request = loanRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        request.setStatus(newStatus);

        // Si se acepta, podrías disparar otras lógicas aquí (como avisar a transporte)
        return loanRequestRepository.save(request);
    }
}