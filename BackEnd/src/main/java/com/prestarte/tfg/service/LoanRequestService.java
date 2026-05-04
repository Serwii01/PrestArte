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
                .orElseThrow(() -> new RuntimeException("Petición no encontrada"));

        request.setStatus(status);
        LoanRequest saved = loanRequestRepository.save(request);

        // --- LÓGICA AUTOMÁTICA DE EMAIL ---
        if (status == LoanRequest.Status.ACEPTADA) {
            notificarAceptacionConContrato(saved);
        }

        return convertToResponse(saved);
    }

    /**
     * Lógica interna para generar el PDF y enviar el correo al Museo
     */
    private void notificarAceptacionConContrato(LoanRequest loan) {
        try {
            // 1. Buscamos si ya existe un envío (Shipment) para el PDF
            Shipment shipment = shipmentService.getByLoanId(loan.getId());

            // 2. Generamos el PDF usando los datos reales (DNI, CIF, etc)
            byte[] pdfBytes = pdfGeneratorService.generateLoanContract(loan, shipment);

            // 3. Datos del envío de email
            String emailDestino = loan.getFoundation().getEmail();
            String asunto = "CONTRATO FORMALIZADO: " + loan.getArtwork().getTitle();
            String cuerpo = "<h2>Petición Aceptada</h2>" +
                    "<p>Estimados responsables de <b>" + loan.getFoundation().getName() + "</b>,</p>" +
                    "<p>Nos complace informarles que el coleccionista ha aceptado el préstamo de la obra.</p>" +
                    "<p>Adjunto a este correo encontrarán el <b>contrato logístico y legal</b> generado por el sistema.</p>" +
                    "<br><p>Atentamente,<br>Equipo Prestarte.</p>";

            String nombreArchivo = "Contrato_" + loan.getArtwork().getTitle().replace(" ", "_") + ".pdf";

            // 4. Disparamos el email
            emailService.sendEmailWithAttachment(emailDestino, asunto, cuerpo, pdfBytes, nombreArchivo);

            System.out.println("Email con contrato enviado correctamente a: " + emailDestino);

        } catch (Exception e) {
            // Usamos un log o print para no interrumpir el flujo si el email falla
            System.err.println("Error crítico al enviar notificación: " + e.getMessage());
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