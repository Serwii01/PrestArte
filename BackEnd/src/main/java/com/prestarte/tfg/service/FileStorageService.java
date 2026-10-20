package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.ConditionReport;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.ExportPermit;
import com.prestarte.tfg.model.entity.FacilityReport;
import com.prestarte.tfg.model.entity.InsuranceCertificate;
import com.prestarte.tfg.model.entity.Message;
import com.prestarte.tfg.model.entity.SupervisionCheckpoint;
import com.prestarte.tfg.model.entity.User;
import com.prestarte.tfg.repository.ArtworkFileRepository;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.ConditionReportRepository;
import com.prestarte.tfg.repository.ExportPermitRepository;
import com.prestarte.tfg.repository.FacilityReportRepository;
import com.prestarte.tfg.repository.InsuranceCertificateRepository;
import com.prestarte.tfg.repository.DBFileRepository;
import com.prestarte.tfg.repository.MessageRepository;
import com.prestarte.tfg.repository.SupervisionCheckpointRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Servicio que centraliza el almacenamiento de archivos binarios.
 *
 * Ofrece operaciones para guardar una fotografía asociada a una obra
 * y para recuperar el contenido de cualquier archivo a partir de su
 * identificador. Los bytes se almacenan en la tabla {@code db_files}
 * a través de {@link DBFile}.
 *
 * La descarga aplica permisos según a qué pertenece el archivo: las
 * fotos de obras y los documentos no confidenciales son públicos; los
 * documentos confidenciales, los de verificación de cuentas, los
 * adjuntos del chat y las fotos de los informes de estado solo los ven
 * las partes implicadas y la administración.
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ArtworkFileRepository artworkFileRepository;
    private final ArtworkRepository artworkRepository;
    private final DBFileRepository dbFileRepository;
    private final MessageRepository messageRepository;
    private final ConditionReportRepository conditionReportRepository;
    private final FacilityReportRepository facilityReportRepository;
    private final FacilityReportService facilityReportService;
    private final InsuranceCertificateRepository insuranceCertificateRepository;
    private final ExportPermitRepository exportPermitRepository;
    private final SupervisionCheckpointRepository supervisionCheckpointRepository;
    private final UserRepository userRepository;
    private final ChatSessionService chatSessionService;
    private final CurrentUser currentUser;

    /**
     * Guarda una imagen asociada a una obra. Crea el {@link DBFile}
     * con los bytes y lo enlaza a la obra mediante un nuevo
     * {@link ArtworkFile} de tipo IMAGE_MAIN. Solo el coleccionista
     * dueño o un administrador pueden añadir fotos.
     */
    @Transactional
    public ArtworkFile storeArtworkFile(Long artworkId, MultipartFile file) throws IOException {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(artwork.getCollector().getId());
        }

        String type = UploadValidator.requireType(file, UploadValidator.IMAGES);
        String fileName = UploadValidator.safeFileName(file.getOriginalFilename(), "imagen");

        DBFile dbFile = DBFile.builder()
                .fileName(fileName)
                .fileType(type)
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();

        ArtworkFile artworkFile = ArtworkFile.builder()
                .artwork(artwork)
                .file(dbFile)
                .type(ArtworkFile.FileType.IMAGE_MAIN)
                .description(fileName)
                .build();

        return artworkFileRepository.save(artworkFile);
    }

    /**
     * Recupera un archivo a partir de su UUID comprobando que el usuario
     * actual (o un visitante anónimo) tiene permiso para verlo.
     */
    @Transactional(readOnly = true)
    public DBFile getFile(String id) {
        DBFile file = dbFileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Archivo", id));
        requireReadAccess(id);
        return file;
    }

    private void requireReadAccess(String fileId) {
        Optional<ArtworkFile> artworkFile = artworkFileRepository.findByFileId(fileId);
        if (artworkFile.isPresent()) {
            ArtworkFile af = artworkFile.get();
            if (!af.isConfidential()) return;
            requireOwnerOrAdmin(af.getArtwork().getCollector().getId());
            return;
        }

        Optional<Message> message = messageRepository.findByAttachmentId(fileId);
        if (message.isPresent()) {
            chatSessionService.requireParticipant(message.get().getChatSession().getLoanRequest());
            return;
        }

        Optional<ConditionReport> report = conditionReportRepository.findByPhotoId(fileId);
        if (report.isPresent()) {
            chatSessionService.requireParticipant(report.get().getShipment().getLoanRequest());
            return;
        }

        Optional<SupervisionCheckpoint> checkpoint = supervisionCheckpointRepository.findByPhotoId(fileId);
        if (checkpoint.isPresent()) {
            chatSessionService.requireParticipant(checkpoint.get().getLoanRequest());
            return;
        }

        Optional<InsuranceCertificate> insurance = insuranceCertificateRepository.findByDocumentId(fileId);
        if (insurance.isPresent()) {
            chatSessionService.requireParticipant(insurance.get().getLoanRequest());
            return;
        }

        Optional<ExportPermit> permit = exportPermitRepository.findByDocumentId(fileId);
        if (permit.isPresent()) {
            chatSessionService.requireParticipant(permit.get().getLoanRequest());
            return;
        }

        Optional<FacilityReport> facility = facilityReportRepository.findByDocumentId(fileId);
        if (facility.isPresent()) {
            facilityReportService.requireReadAccess(facility.get().getFoundation().getId());
            return;
        }

        Optional<User> owner = userRepository.findByVerificationFileId(fileId);
        if (owner.isPresent()) {
            requireOwnerOrAdmin(owner.get().getId());
            return;
        }

        // Archivo huérfano: solo la administración.
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("No tienes permiso para ver este archivo");
        }
    }

    private void requireOwnerOrAdmin(Long ownerId) {
        if (currentUser.isAdmin()) return;
        currentUser.requireUserId(ownerId);
    }
}
