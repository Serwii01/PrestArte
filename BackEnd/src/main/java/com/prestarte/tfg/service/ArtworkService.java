package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.ArtworkDto;
import com.prestarte.tfg.model.dto.ArtworkPage;
import com.prestarte.tfg.model.dto.ArtworkSearchCriteria;
import com.prestarte.tfg.model.dto.BusyPeriod;
import com.prestarte.tfg.model.dto.CreateArtworkRequest;
import com.prestarte.tfg.model.dto.FileDto;
import com.prestarte.tfg.model.dto.UpdateArtworkRequest;
import com.prestarte.tfg.model.entity.Artwork;
import com.prestarte.tfg.model.entity.ArtworkFile;
import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.model.entity.CourierMode;
import com.prestarte.tfg.model.entity.DBFile;
import com.prestarte.tfg.model.entity.LoanRequest;
import com.prestarte.tfg.model.entity.LoanRequirements;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.model.entity.Shipment;
import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.ArtworkFileRepository;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.CollectorRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ShipmentRepository;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio que gestiona las obras del catálogo.
 *
 * Cubre la creación, edición, eliminación y deshabilitación temporal de
 * las obras, junto con la subida y borrado de documentos adjuntos
 * (seguros, certificados, informes).
 *
 * La conversión a DTO protege la privacidad del coleccionista: a quien
 * no es el dueño ni un administrador se le ocultan los documentos
 * confidenciales, el valor exacto (los usuarios verificados ven solo una
 * horquilla y los visitantes nada), la dirección de recogida (salvo al
 * transportista con un envío en curso) y, si la obra es anónima, la
 * identidad del coleccionista.
 */
@Service
@RequiredArgsConstructor
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkFileRepository artworkFileRepository;
    private final CollectorRepository collectorRepository;
    private final TransportCompanyRepository transportCompanyRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUser currentUser;

    /** Nombre que ven los terceros cuando la obra es anónima. */
    public static final String ANONYMOUS_OWNER = "Colección particular";

    /** Estados del envío en los que el transportista necesita la dirección de recogida. */
    private static final Set<Shipment.ShipmentStatus> PICKUP_STATUSES = EnumSet.of(
            Shipment.ShipmentStatus.APPROVED,
            Shipment.ShipmentStatus.PICKED_UP,
            Shipment.ShipmentStatus.IN_TRANSIT);

    /**
     * Crea una nueva obra para el coleccionista indicado.
     *
     * Resuelve además la empresa de transporte preferida en caso de
     * que se haya indicado y persiste el estado de conservación a
     * partir del valor que llega validado en el DTO.
     */
    @Transactional
    public ArtworkDto createArtwork(CreateArtworkRequest request) {
        // Solo se pueden crear obras propias (o cualquiera, si eres admin).
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(request.getCollectorId());
        }
        if (!Boolean.TRUE.equals(request.getTitleDeclared())) {
            throw new IllegalArgumentException("Para publicar una obra debes firmar la declaración de titularidad.");
        }
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
                .requirements(cleanRequirements(request.getRequirements()))
                .pickupAddress(blankToNull(request.getPickupAddress()))
                .showOwnerName(request.isShowOwnerName())
                .heritageStatus(request.getHeritageStatus() != null
                        ? request.getHeritageStatus() : Artwork.HeritageStatus.NONE)
                .titleDeclaredAt(Boolean.TRUE.equals(request.getTitleDeclared()) ? LocalDateTime.now() : null)
                .creditLine(blankToNull(request.getCreditLine()))
                .imageRights(request.getImageRights())
                .loanFee(request.getLoanFee() != null && request.getLoanFee() > 0 ? request.getLoanFee() : null)
                .dueDiligenceStatus(Artwork.DueDiligenceStatus.PENDING)
                .visibility(request.getVisibility() != null
                        ? request.getVisibility() : Artwork.Visibility.PUBLIC)
                .collector(collector)
                .preferredTransportCompany(preferred)
                .preferredTransportMandatory(
                        preferred != null && request.isPreferredTransportMandatory())
                .build();
        artwork.setCondition(Artwork.Condition.valueOf(request.getCondition()));

        Artwork saved = artworkRepository.save(artwork);
        return convertToDto(saved);
    }

    /**
     * Devuelve el catálogo. Los visitantes sin sesión solo ven las obras
     * públicas; las marcadas como MEMBERS solo aparecen a usuarios
     * verificados.
     */
    @Transactional(readOnly = true)
    public List<ArtworkDto> getAllArtworks() {
        Viewer viewer = viewer();
        return artworkRepository.findAll().stream()
                .filter(a -> visibleTo(a, viewer))
                .map(a -> convertToDto(a, viewer))
                .collect(Collectors.toList());
    }

    /**
     * Búsqueda del catálogo con filtros, orden y paginación.
     *
     * Filtra en memoria: suficiente para el volumen actual. Si el catálogo
     * crece a miles de obras, conviene pasarlo a consultas JPA (Specification).
     */
    @Transactional(readOnly = true)
    public ArtworkPage search(ArtworkSearchCriteria c) {
        Viewer viewer = viewer();
        String q = normalize(c.getQ());
        String artist = normalize(c.getArtist());
        String city = normalize(c.getCity());
        boolean dateFilter = c.getAvailableFrom() != null && c.getAvailableTo() != null
                && c.getAvailableTo().isAfter(c.getAvailableFrom());

        List<Artwork> filtered = artworkRepository.findAll().stream()
                .filter(a -> visibleTo(a, viewer))
                .filter(a -> q == null || contains(a.getTitle(), q) || contains(a.getArtist(), q)
                        || contains(a.getDescription(), q) || contains(a.getLocation(), q))
                .filter(a -> artist == null || contains(a.getArtist(), artist))
                .filter(a -> city == null || contains(a.getLocation(), city))
                .filter(a -> c.getYearFrom() == null || (a.getYear() != null && a.getYear() >= c.getYearFrom()))
                .filter(a -> c.getYearTo() == null || (a.getYear() != null && a.getYear() <= c.getYearTo()))
                .filter(a -> c.getCondition() == null || c.getCondition().isBlank()
                        || (a.getCondition() != null && a.getCondition().name().equals(c.getCondition())))
                .filter(a -> !c.isVerifiedOnly() || ProvenanceService.status(a) == Artwork.DueDiligenceStatus.CLEAR)
                .filter(a -> !c.isLendableOnly() || (a.isAvailableForLoan() && a.getTitleDeclaredAt() != null))
                .filter(a -> !dateFilter || !loanRequestRepository.existsOverlappingLoan(
                        a.getId(), null, c.getAvailableFrom(), c.getAvailableTo()))
                .sorted(comparatorFor(c.getSort()))
                .toList();

        int size = Math.max(1, Math.min(c.getSize(), 60));
        int page = Math.max(0, c.getPage());
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<ArtworkDto> items = filtered.subList(from, to).stream().map(a -> convertToDto(a, viewer)).toList();
        int totalPages = (int) Math.ceil(filtered.size() / (double) size);
        return new ArtworkPage(items, filtered.size(), page, size, totalPages);
    }

    /**
     * Periodos en los que la obra ya está comprometida con un préstamo,
     * para pintar su calendario de disponibilidad. No revela quién la pidió.
     */
    @Transactional(readOnly = true)
    public List<BusyPeriod> availability(Long id) {
        Viewer viewer = viewer();
        artworkRepository.findById(id)
                .filter(a -> visibleTo(a, viewer))
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        return loanRequestRepository.findByArtworkId(id).stream()
                .filter(l -> LoanRequestRepository.COMMITTED_STATUSES.contains(l.getStatus()))
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .sorted(Comparator.comparing(LoanRequest::getStartDate))
                .map(l -> new BusyPeriod(l.getStartDate(), l.getEndDate()))
                .toList();
    }

    private static Comparator<Artwork> comparatorFor(String sort) {
        Comparator<Artwork> recent = Comparator.comparing(Artwork::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
        if (sort == null) return recent;
        return switch (sort) {
            case "title" -> Comparator.comparing((Artwork a) -> a.getTitle().toLowerCase());
            case "artist" -> Comparator.comparing((Artwork a) -> a.getArtist().toLowerCase());
            case "year" -> Comparator.comparing(Artwork::getYear, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> recent;
        };
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) return null;
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private static boolean contains(String haystack, String needle) {
        String h = normalize(haystack);
        return h != null && h.contains(needle);
    }

    /** Devuelve la ficha de una obra, respetando su visibilidad. */
    @Transactional(readOnly = true)
    public ArtworkDto getArtworkById(Long id) {
        Viewer viewer = viewer();
        Artwork artwork = artworkRepository.findById(id)
                .filter(a -> visibleTo(a, viewer))
                .orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
        return convertToDto(artwork, viewer);
    }

    /**
     * Devuelve todas las obras de un coleccionista. Solo para el propio
     * coleccionista o la administración: de lo contrario permitiría
     * relacionar entre sí las obras de un coleccionista anónimo.
     */
    @Transactional(readOnly = true)
    public List<ArtworkDto> getArtworksByCollector(Long collectorId) {
        if (!currentUser.isAdmin()) {
            currentUser.requireUserId(collectorId);
        }
        Viewer viewer = viewer();
        return artworkRepository.findByCollectorId(collectorId).stream()
                .map(a -> convertToDto(a, viewer))
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
        if (req.getPickupAddress() != null) a.setPickupAddress(blankToNull(req.getPickupAddress()));
        if (req.getRequirements() != null) a.setRequirements(cleanRequirements(req.getRequirements()));
        if (req.getShowOwnerName() != null) a.setShowOwnerName(req.getShowOwnerName());
        if (req.getVisibility() != null) a.setVisibility(req.getVisibility());
        if (req.getHeritageStatus() != null) a.setHeritageStatus(req.getHeritageStatus());
        if (req.getCreditLine() != null) a.setCreditLine(blankToNull(req.getCreditLine()));
        if (req.getImageRights() != null) a.setImageRights(req.getImageRights());
        if (req.getLoanFee() != null) a.setLoanFee(req.getLoanFee() > 0 ? req.getLoanFee() : null);
        if (Boolean.TRUE.equals(req.getTitleDeclared()) && a.getTitleDeclaredAt() == null
                && currentUser.isAnyOf(a.getCollector().getId())) {
            a.setTitleDeclaredAt(LocalDateTime.now());
        }

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

        String type = UploadValidator.requireType(file, UploadValidator.IMAGES_AND_PDF);
        String name = UploadValidator.safeFileName(file.getOriginalFilename(), "documento");

        DBFile dbFile = DBFile.builder()
                .fileName(name)
                .fileType(type)
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
     * Quién está mirando: id (o null si es un visitante), si es
     * administrador y, si es un transportista, las obras para las que
     * tiene un envío aprobado y en curso (necesita su dirección).
     */
    private record Viewer(Long id, boolean admin, Set<Long> pickupArtworkIds) {
        boolean isMember() {
            return id != null;
        }

        boolean owns(Artwork a) {
            return id != null && a.getCollector() != null && id.equals(a.getCollector().getId());
        }
    }

    private Viewer viewer() {
        Long id = currentUser.currentIdOrNull();
        Role role = currentUser.roleOrNull();
        Set<Long> pickupIds = Set.of();
        if (id != null && role == Role.TRANSPORT) {
            pickupIds = shipmentRepository.findByTransportCompanyId(id).stream()
                    .filter(s -> PICKUP_STATUSES.contains(s.getStatus()))
                    .map(s -> s.getLoanRequest().getArtwork().getId())
                    .collect(Collectors.toSet());
        }
        return new Viewer(id, role == Role.ADMIN, pickupIds);
    }

    /**
     * Una obra se ve si es pública (o quien mira tiene sesión) y no está
     * retirada por la diligencia debida; las retiradas solo las ven el
     * dueño y la administración.
     */
    private static boolean visibleTo(Artwork a, Viewer viewer) {
        if (viewer.admin() || viewer.owns(a)) return true;
        if (ProvenanceService.status(a) == Artwork.DueDiligenceStatus.FLAGGED) return false;
        // Obras de coleccionistas que se han dado de baja.
        if (a.getCollector() != null && a.getCollector().getDeletedAt() != null) return false;
        return viewer.isMember() || isPublic(a);
    }

    private static boolean isPublic(Artwork a) {
        return a.getVisibility() == null || a.getVisibility() == Artwork.Visibility.PUBLIC;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Valida los requisitos de préstamo y normaliza: las casillas no
     * marcadas se guardan como nulo ("sin requisito").
     */
    private static LoanRequirements cleanRequirements(LoanRequirements r) {
        if (r == null) return null;
        checkRange(r.getTempMin(), r.getTempMax(), -10, 40, "temperatura");
        checkRange(r.getRhMin(), r.getRhMax(), 0, 100, "humedad relativa");
        if (r.getMaxLux() != null && r.getMaxLux() < 0) {
            throw new IllegalArgumentException("Los lux no pueden ser negativos.");
        }
        if (r.getMinNoticeDays() != null && (r.getMinNoticeDays() < 0 || r.getMinNoticeDays() > 1095)) {
            throw new IllegalArgumentException("La antelación mínima debe estar entre 0 y 1095 días.");
        }
        return LoanRequirements.builder()
                .tempMin(r.getTempMin())
                .tempMax(r.getTempMax())
                .rhMin(r.getRhMin())
                .rhMax(r.getRhMax())
                .maxLux(r.getMaxLux())
                .climateControl(trueOrNull(r.getClimateControl()))
                .displayCase(trueOrNull(r.getDisplayCase()))
                .guards24h(trueOrNull(r.getGuards24h()))
                .cctv(trueOrNull(r.getCctv()))
                .intrusionAlarm(trueOrNull(r.getIntrusionAlarm()))
                .fireDetection(trueOrNull(r.getFireDetection()))
                .minNoticeDays(r.getMinNoticeDays())
                .courierMode(r.getCourierMode() == CourierMode.NONE ? null : r.getCourierMode())
                .build();
    }

    private static void checkRange(Double min, Double max, double lo, double hi, String what) {
        if ((min != null && (min < lo || min > hi)) || (max != null && (max < lo || max > hi))) {
            throw new IllegalArgumentException("Valor de " + what + " fuera de rango.");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("El mínimo de " + what + " no puede ser mayor que el máximo.");
        }
    }

    private static Boolean trueOrNull(Boolean b) {
        return Boolean.TRUE.equals(b) ? Boolean.TRUE : null;
    }

    /** Horquilla de valor que se muestra en lugar del importe exacto. */
    static String valueRange(Double value) {
        if (value == null) return null;
        double v = value;
        if (v < 10_000) return "Menos de 10.000 €";
        if (v < 50_000) return "10.000 – 50.000 €";
        if (v < 100_000) return "50.000 – 100.000 €";
        if (v < 250_000) return "100.000 – 250.000 €";
        if (v < 500_000) return "250.000 – 500.000 €";
        if (v < 1_000_000) return "500.000 – 1.000.000 €";
        if (v < 5_000_000) return "1 – 5 millones €";
        return "Más de 5 millones €";
    }

    /** Conversión para las operaciones del propio dueño (crear, editar...). */
    private ArtworkDto convertToDto(Artwork artwork) {
        return convertToDto(artwork, viewer());
    }

    /**
     * Transforma una obra en su DTO aplicando las reglas de privacidad
     * según quién mira (ver la documentación de la clase).
     */
    private ArtworkDto convertToDto(Artwork artwork, Viewer viewer) {
        Long ownerId = artwork.getCollector() != null ? artwork.getCollector().getId() : null;
        boolean privileged = viewer.admin() || viewer.owns(artwork);
        boolean showOwner = privileged || artwork.isShowOwnerName();
        boolean seesPickup = privileged || viewer.pickupArtworkIds().contains(artwork.getId());

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
                .estimatedValue(privileged ? artwork.getEstimatedValue() : null)
                .estimatedValueRange(viewer.isMember() ? valueRange(artwork.getEstimatedValue()) : null)
                .loanConditions(artwork.getLoanConditions())
                .requirements(artwork.getRequirements())
                .location(artwork.getLocation())
                .pickupAddress(seesPickup ? artwork.getPickupAddress() : null)
                .collectorId(showOwner ? ownerId : null)
                .collectorName(showOwner && artwork.getCollector() != null
                        ? artwork.getCollector().getName() : ANONYMOUS_OWNER)
                .showOwnerName(privileged ? artwork.isShowOwnerName() : null)
                .visibility(privileged
                        ? (isPublic(artwork) ? Artwork.Visibility.PUBLIC : Artwork.Visibility.MEMBERS).name()
                        : null)
                .preferredTransportCompanyId(preferred != null ? preferred.getId() : null)
                .preferredTransportCompanyName(preferred != null ? preferred.getCompanyName() : null)
                .preferredTransportMandatory(artwork.isPreferredTransportMandatory())
                .heritageStatus(artwork.getHeritageStatus() != null
                        ? artwork.getHeritageStatus().name() : Artwork.HeritageStatus.NONE.name())
                .titleDeclared(artwork.getTitleDeclaredAt() != null)
                .dueDiligenceStatus(ProvenanceService.status(artwork).name())
                .creditLine(artwork.getCreditLine())
                .imageRights(artwork.getImageRights())
                .loanFee(artwork.getLoanFee())
                .availableForLoan(artwork.isAvailableForLoan())
                .files(fileDtos)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
