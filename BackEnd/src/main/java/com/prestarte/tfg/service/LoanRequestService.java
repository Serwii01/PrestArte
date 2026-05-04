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

    // Inyecciones para la funcionalidad de Contrato y Email
    private final PdfGeneratorService pdfGeneratorService;
    private final EmailService emailService;
    private final ShipmentService shipmentService;

    @Transactional
    public LoanResponse createRequest(CreateLoanRequest dto) {
        Artwork artwork = artworkRepository.findById(dto.getArtworkId())
                .orElseThrow(() -> new RuntimeException("Obra no encontrada"));

        Foundation foundation = foundationRepository.findById(dto.getFoundationId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        LoanRequest request = LoanRequest.builder()
                .artwork(artwork)
                .foundation(foundation)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(LoanRequest.Status.PENDIENTE)
                .build();

        LoanRequest saved = loanRequestRepository.save(request);

        return convertToResponse(saved);
    }

    public List<LoanRequest> getRequestsByFoundation(Long foundationId) {
        return loanRequestRepository.findByFoundationId(foundationId);
    }

    public List<LoanRequest> getRequestsByCollector(Long collectorId) {
        return loanRequestRepository.findByArtworkCollectorId(collectorId);
    }


    @Transactional
    public LoanResponse updateStatus(Long loanId, LoanRequest.Status status) {
        LoanRequest request = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan request not found"));

        // VALIDATION: If the status is being changed to ACCEPTED
        if (status == LoanRequest.Status.ACEPTADA) {
            boolean isOverlapping = loanRequestRepository.existsOverlappingLoan(
                    request.getArtwork().getId(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            if (isOverlapping) {
                // This will roll back the transaction and return an error to the user
                throw new RuntimeException("CONFLICT: This artwork is already booked for the selected dates.");
            }

            // If not overlapping, proceed and notify parties
            request.setStatus(status);
            LoanRequest saved = loanRequestRepository.save(request);
            notifyPartiesOnAcceptance(saved);
            return convertToResponse(saved);
        }

        // For other status changes (REJECTED, etc.)
        request.setStatus(status);
        LoanRequest saved = loanRequestRepository.save(request);
        return convertToResponse(saved);
    }

    /**
     * Lógica interna para generar el PDF y enviar el correo al Museo
     */
    /**
     * Notifies all three parties (Foundation, Collector, and Transport Company)
     * by sending the formal contract via email.
     */
    private void notifyPartiesOnAcceptance(LoanRequest loan) {
        try {
            // 1. Fetch shipment and generate the PDF document
            Shipment shipment = shipmentService.getByLoanId(loan.getId());
            byte[] pdfBytes = pdfGeneratorService.generateLoanContract(loan, shipment);

            String fileName = "Contract_" + loan.getArtwork().getTitle().replace(" ", "_") + ".pdf";
            String subject = "LOAN CONTRACT FORMALIZED: " + loan.getArtwork().getTitle();

            // --- 1. NOTIFICATION TO THE FOUNDATION (MUSEUM) ---
            String foundationEmail = loan.getFoundation().getEmail();
            if (foundationEmail != null) {
                String foundationBody = "<h3>Loan Contract - Recipient Copy</h3>" +
                        "<p>Dear team at <b>" + loan.getFoundation().getName() + "</b>,</p>" +
                        "<p>The loan request has been officially accepted. Please find the attached legal contract.</p>";
                emailService.sendEmailWithAttachment(foundationEmail, subject, foundationBody, pdfBytes, fileName);
            }

            // --- 2. NOTIFICATION TO THE COLLECTOR (OWNER) ---
            String collectorEmail = loan.getArtwork().getCollector().getEmail();
            if (collectorEmail != null) {
                String collectorBody = "<h3>Loan Contract - Owner Copy</h3>" +
                        "<p>Hello <b>" + loan.getArtwork().getCollector().getName() + "</b>,</p>" +
                        "<p>You have accepted the loan request for <i>" + loan.getArtwork().getTitle() + "</i>. " +
                        "Attached is your copy of the signed agreement.</p>";
                emailService.sendEmailWithAttachment(collectorEmail, subject, collectorBody, pdfBytes, fileName);
            }

            // --- 3. NOTIFICATION TO THE TRANSPORT COMPANY ---
            if (shipment != null && shipment.getTransportCompany() != null) {
                String transportEmail = shipment.getTransportCompany().getEmail();
                if (transportEmail != null) {
                    String transportBody = "<h3>Art Transport Order</h3>" +
                            "<p>A new transport service has been assigned for: <b>" + loan.getArtwork().getTitle() + "</b>.</p>" +
                            "<p>Please find the attached contract containing all safety and handling requirements.</p>";
                    emailService.sendEmailWithAttachment(transportEmail, "NEW SERVICE: " + loan.getArtwork().getTitle(), transportBody, pdfBytes, fileName);
                }
            }

            System.out.println("Multi-party notification flow completed successfully.");

        } catch (Exception e) {
            // We log the error but don't break the main transaction
            System.err.println("Error sending multi-party notifications: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public LoanRequest getById(Long id) {
        return loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la petición de préstamo con ID: " + id));
    }

    public List<LoanRequest> getAllLoanRequests() {
        return loanRequestRepository.findAll();
    }

    @Transactional
    public LoanRequest save(LoanRequest loanRequest) {
        return loanRequestRepository.save(loanRequest);
    }

    // Método auxiliar para no repetir código de mapeo
    private LoanResponse convertToResponse(LoanRequest saved) {
        return LoanResponse.builder()
                .id(saved.getId())
                .artworkTitle(saved.getArtwork().getTitle())
                .foundationName(saved.getFoundation().getName())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus().name())
                .build();
    }
}