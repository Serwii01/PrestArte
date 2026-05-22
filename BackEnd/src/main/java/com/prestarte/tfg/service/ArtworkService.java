package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.FileDto;
import com.prestarte.tfg.model.dto.UpdateArtworkRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.ArtworkFileRepository;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona las obras del catálogo.
 *
 * Cubre la creación, edición, eliminación y deshabilitación temporal de
 * las obras, junto con la subida y borrado de documentos adjuntos
 * (seguros, certificados, informes). La conversión a DTO filtra los
 * documentos marcados como confidenciales cuando el usuario que
 * consulta no es el dueño ni un administrador.
 */
@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkFileRepository artworkFileRepository;
    private final CollectorRepository collectorRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final CurrentUser currentUser;

    /**
     * Crea una nueva obra para el coleccionista indicado.
     *
     * Resuelve además la empresa de transporte preferida en caso de
     * que se haya indicado y persiste el estado de conservación a
     * partir del valor que llega validado en el DTO.
     */
    @Transactional
    public ArtworkDto createArtwork(CreateArtworkRequest request) {
        Collector collector = collectorRepository.findById(request.getCollectorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Coleccionista", request.getCollectorId()));

        TransportCompany preferred = null;
        if (request.getPreferredTransportCompanyId() != null) {
            preferred = transportCompanyRepository.findById(request.getPreferredTransportCompanyId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Empresa de transporte", request.getPreferredTransportCompanyId()));
        }

        Artwork artwork = Artwork.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .year(request.getYear())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .depthCm(request.getDepthCm())
                .description(request.getDescription())
                .estimatedValue(request.getEstimatedValue())
                .loanConditions(request.getLoanConditions())
                .location(request.getLocation())
                .collector(collector)
                .preferredTransportCompany(preferred)
                .preferredTransportMandatory(
                        preferred != null && request.isPreferredTransportMandatory())
                .build();
        artwork.setCondition(Artwork.Condition.valueOf(request.getCondition()));

        Artwork saved = artworkRepository.save(artwork);
        return convertToDto(saved);
    }

    /** Devuelve el catálogo completo de obras. */
    public List<ArtworkDto> getAllArtworks() {
        return artworkRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /** Devuelve la ficha de una obra a partir de su identificador. */
    public ArtworkDto getArtworkById(Long id) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        return convertToDto(artwork);
    }

    /** Devuelve todas las obras pertenecientes a un coleccionista. */
    public List<ArtworkDto> getArtworksByCollector(Long collectorId) {
        return artworkRepository.findByCollectorId(collectorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Edita una obra existente. La acción queda reservada al
     * coleccionista propietario o a un administrador. Solo se
     * sobreescriben los campos que vengan informados en la petición.
     */
    @Transactional
    public ArtworkDto updateArtwork(Long id, UpdateArtworkRequest req) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);

        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getArtist() != null) a.setArtist(req.getArtist());
        if (req.getYear() != null) a.setYear(req.getYear());
        if (req.getWidthCm() != null) a.setWidthCm(req.getWidthCm());
        if (req.getHeightCm() != null) a.setHeightCm(req.getHeightCm());
        if (req.getDepthCm() != null) a.setDepthCm(req.getDepthCm());
        if (req.getCondition() != null) a.setCondition(Artwork.Condition.valueOf(req.getCondition()));
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        if (req.getEstimatedValue() != null) a.setEstimatedValue(req.getEstimatedValue());
        if (req.getLoanConditions() != null) a.setLoanConditions(req.getLoanConditions());
        if (req.getLocation() != null) a.setLocation(req.getLocation());

        if (req.getPreferredTransportCompanyId() != null) {
            TransportCompany tc = transportCompanyRepository.findById(req.getPreferredTransportCompanyId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Empresa de transporte", req.getPreferredTransportCompanyId()));
            a.setPreferredTransportCompany(tc);
        }
        if (req.getPreferredTransportMandatory() != null) {
            a.setPreferredTransportMandatory(req.getPreferredTransportMandatory());
        }

        return convertToDto(artworkRepository.save(a));
    }

    /**
     * Habilita o deshabilita la obra para nuevas solicitudes de
     * préstamo. La obra deshabilitada sigue siendo visible en el
     * catálogo, pero las fundaciones no pueden iniciar una solicitud
     * sobre ella hasta que el coleccionista la vuelva a habilitar.
     */
    @Transactional
    public ArtworkDto setAvailability(Long id, boolean available) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);
        a.setAvailableForLoan(available);
        return convertToDto(artworkRepository.save(a));
    }

    /**
     * Elimina una obra del catálogo. La eliminación solo se permite
     * cuando la obra no tiene préstamos vivos, para preservar el
     * histórico de transacciones realizadas.
     */
    @Transactional
    public void deleteArtwork(Long id) {
        Artwork a = artworkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        requireOwnership(a);

        boolean hasActive = loanRequestRepository.findByArtworkCollectorId(a.getCollector().getId())
                .stream()
                .filter(l -> l.getArtwork().getId().equals(id))
                .anyMatch(l -> {
                    LoanRequest.Status s = l.getStatus();
                    return s != LoanRequest.Status.RETURNED
                        && s != LoanRequest.Status.REJECTED
                        && s != LoanRequest.Status.CANCELLED;
                });
        if (hasActive) {
            throw new IllegalStateException(
                    "No se puede eliminar la obra porque tiene préstamos activos. " +
                    "Dala de baja primero para evitar nuevas solicitudes.");
        }
        artworkRepository.delete(a);
    }

    // ===== Documentación adjunta a la obra =====

    /**
     * Asocia un documento (certificado, seguro, informe...) a la obra
     * indicada. La acción está reservada al coleccionista dueño o a un
     * administrador. Si el documento se marca como confidencial, solo
     * ellos podrán verlo en la ficha pública.
     */
    @Transactional
    public ArtworkDto addDocument(Long artworkId, String description, boolean confidential,
                                  MultipartFile file) throws IOException {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        requireOwnership(artwork);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio.");
        }
        String name = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename());

        DBFile dbFile = DBFile.builder()
                .fileName(name)
                .fileType(file.getContentType())
                .data(file.getBytes())
                .fileSize(file.getSize())
                .build();

        ArtworkFile af = ArtworkFile.builder()
                .artwork(artwork)
                .file(dbFile)
                .type(ArtworkFile.FileType.DOCUMENT)
                .description(description != null && !description.isBlank() ? description : name)
                .confidential(confidential)
                .build();

        artworkFileRepository.save(af);
        return convertToDto(artworkRepository.findById(artworkId).orElseThrow());
    }

    /**
     * Elimina un documento adjunto de la obra. Solo el dueño o un
     * administrador pueden hacerlo, y la operación se rechaza si el
     * archivo apuntado no es un documento (las fotografías se gestionan
     * por un flujo distinto).
     */
    @Transactional
    public ArtworkDto deleteDocument(Long artworkId, Long artworkFileId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", artworkId));
        requireOwnership(artwork);

        ArtworkFile af = artworkFileRepository.findById(artworkFileId)
                .orElseThrow(() -> ResourceNotFoundException.of("Documento", artworkFileId));

        if (!Objects.equals(af.getArtwork().getId(), artworkId)) {
            throw new AccessDeniedException("El documento no pertenece a esta obra.");
        }
        if (af.getType() != ArtworkFile.FileType.DOCUMENT) {
            throw new IllegalStateException(
                    "Este endpoint solo elimina documentos, no fotos.");
        }
        artworkFileRepository.delete(af);
        return convertToDto(artworkRepository.findById(artworkId).orElseThrow());
    }

    // ===== Helpers privados =====

    /** Comprueba que la sesión actual es la del coleccionista dueño o un administrador. */
    private void requireOwnership(Artwork a) {
        if (currentUser.isAdmin()) return;
        currentUser.requireUserId(a.getCollector().getId());
    }

    /**
     * Transforma una obra en su DTO público, ocultando los documentos
     * marcados como confidenciales cuando el usuario que mira no es el
     * dueño ni un administrador.
     */
    private ArtworkDto convertToDto(Artwork artwork) {
        Long ownerId = artwork.getCollector() != null ? artwork.getCollector().getId() : null;
        Long viewerId = currentUser.currentIdOrNull();
        boolean privileged = currentUser.isAdminOrNull()
                || (viewerId != null && viewerId.equals(ownerId));

        List<FileDto> fileDtos = new ArrayList<>();
        if (artwork.getFiles() != null) {
            fileDtos = artwork.getFiles().stream()
                    .filter(af -> privileged || !af.isConfidential())
                    .map(af -> FileDto.builder()
                            .id(af.getFile().getId())
                            .artworkFileId(af.getId())
                            .fileName(af.getFile().getFileName())
                            .fileType(af.getFile().getFileType())
                            .fileSize(af.getFile().getFileSize())
                            .type(af.getType() != null ? af.getType().name() : null)
                            .description(af.getDescription())
                            .confidential(af.isConfidential())
                            .build())
                    .collect(Collectors.toList());
        }

        TransportCompany preferred = artwork.getPreferredTransportCompany();

        return ArtworkDto.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .artist(artwork.getArtist())
                .year(artwork.getYear())
                .widthCm(artwork.getWidthCm())
                .heightCm(artwork.getHeightCm())
                .depthCm(artwork.getDepthCm())
                .condition(artwork.getCondition() != null ? artwork.getCondition().name() : null)
                .description(artwork.getDescription())
                .estimatedValue(artwork.getEstimatedValue())
                .loanConditions(artwork.getLoanConditions())
                .location(artwork.getLocation())
                .collectorId(ownerId)
                .collectorName(artwork.getCollector() != null ? artwork.getCollector().getName() : "Anónim@")
                .preferredTransportCompanyId(preferred != null ? preferred.getId() : null)
                .preferredTransportCompanyName(preferred != null ? preferred.getCompanyName() : null)
                .preferredTransportMandatory(artwork.isPreferredTransportMandatory())
                .availableForLoan(artwork.isAvailableForLoan())
                .files(fileDtos)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
