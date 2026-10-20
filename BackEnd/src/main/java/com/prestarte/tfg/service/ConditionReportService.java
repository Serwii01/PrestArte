package com.prestarte.tfg.service;

import com.prestarte.tfg.model.dto.ConditionReportRequest;
import com.prestarte.tfg.model.dto.ConditionReportResponse;
import com.prestarte.tfg.model.entity.ConditionReport;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.ConditionReportRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea y consulta los informes de estado de las obras.
 *
 * No comprueba permisos por sí mismo: lo llaman {@link ShipmentService}
 * (que ya ha validado quién recoge o entrega) y {@link LoanRequestService}
 * (que ya ha validado el acceso al préstamo).
 */
@Service
@RequiredArgsConstructor
public class ConditionReportService {

    /** Mínimo de fotos exigido: sin fotos el informe no prueba nada. */
    public static final int MIN_PHOTOS = 1;
    public static final int MAX_PHOTOS = 10;

    private final ConditionReportRepository conditionReportRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    /** Valida y guarda el informe de estado de un hito del envío. */
    @Transactional
    public ConditionReport create(Shipment shipment, ConditionReport.Stage stage,
                                  ConditionReportRequest data, List<MultipartFile> photos) throws IOException {
        if (data == null || data.getCondition() == null) {
            throw new IllegalArgumentException("Indica el estado de conservación de la obra.");
        }
        List<MultipartFile> files = photos == null ? List.of()
                : photos.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.size() < MIN_PHOTOS) {
            throw new IllegalArgumentException("Adjunta al menos una fotografía de la obra.");
        }
        if (files.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Puedes adjuntar como máximo " + MAX_PHOTOS + " fotografías.");
        }

        List<DBFile> dbFiles = new ArrayList<>();
        for (MultipartFile f : files) {
            String type = UploadValidator.requireType(f, UploadValidator.IMAGES);
            dbFiles.add(DBFile.builder()
                    .fileName(UploadValidator.safeFileName(f.getOriginalFilename(), "foto"))
                    .fileType(type)
                    .data(f.getBytes())
                    .fileSize(f.getSize())
                    .build());
        }

        User author = userRepository.getReferenceById(currentUser.currentId());
        String notes = data.getNotes() != null && !data.getNotes().isBlank() ? data.getNotes().trim() : null;

        return conditionReportRepository.save(ConditionReport.builder()
                .shipment(shipment)
                .stage(stage)
                .condition(data.getCondition())
                .damageReported(data.isDamageReported())
                .notes(notes)
                .photos(dbFiles)
                .author(author)
                .build());
    }

    /** Informes de todos los envíos de un préstamo. El llamante comprueba el acceso. */
    @Transactional(readOnly = true)
    public List<ConditionReportResponse> getByLoan(Long loanId) {
        return conditionReportRepository.findByShipmentLoanRequestIdOrderByCreatedAtAscIdAsc(loanId).stream()
                .map(r -> ConditionReportResponse.builder()
                        .id(r.getId())
                        .shipmentId(r.getShipment().getId())
                        .direction(r.getShipment().getDirection() != null
                                ? r.getShipment().getDirection().name() : null)
                        .stage(r.getStage().name())
                        .condition(r.getCondition().name())
                        .damageReported(r.isDamageReported())
                        .notes(r.getNotes())
                        .authorName(r.getAuthor().getName())
                        .authorRole(r.getAuthor().getRole().name())
                        .photoIds(r.getPhotos().stream().map(DBFile::getId).toList())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }
}
