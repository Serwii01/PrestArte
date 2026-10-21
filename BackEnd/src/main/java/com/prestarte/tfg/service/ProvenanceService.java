package com.prestarte.tfg.service;

import com.prestarte.tfg.exception.ResourceNotFoundException;
import com.prestarte.tfg.model.dto.DueDiligencePendingItem;
import com.prestarte.tfg.model.dto.DueDiligenceRequest;
import com.prestarte.tfg.model.dto.ProvenanceEntryRequest;
import com.prestarte.tfg.model.dto.ProvenanceResponse;
import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.ArtworkRepository;
import com.prestarte.tfg.repository.DueDiligenceCheckRepository;
import com.prestarte.tfg.repository.LoanRequestRepository;
import com.prestarte.tfg.repository.ProvenanceEntryRepository;
import com.prestarte.tfg.repository.UserRepository;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Titularidad y procedencia de las obras.
 *
 * <ul>
 *   <li><b>Declaración de titularidad</b>: el coleccionista declara que la
 *       obra es suya (o que está autorizado) y que no le consta robada,
 *       expoliada ni exportada ilícitamente. Sin ella, la obra no recibe
 *       solicitudes.</li>
 *   <li><b>Historial de procedencia</b>: propietarios anteriores con sus
 *       justificantes. Se detectan los huecos y se resaltan los que caen
 *       entre 1933 y 1945, el periodo del expolio nazi al que los museos
 *       prestan especial atención (Principios de Washington, 1998).</li>
 *   <li><b>Diligencia debida</b>: la administración comprueba la obra en
 *       bases de datos de obras robadas y anota el resultado. Una obra
 *       marcada como sospechosa sale del catálogo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProvenanceService {

    /** Periodo de expolio nazi que los museos revisan con especial cuidado. */
    static final int SPOLIATION_FROM = 1933;
    static final int SPOLIATION_TO = 1945;

    private final ArtworkRepository artworkRepository;
    private final ProvenanceEntryRepository entryRepository;
    private final DueDiligenceCheckRepository checkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CurrentUser currentUser;

    // ===== Consulta =====

    /** Procedencia de una obra. Requiere sesión: no se publica en el catálogo abierto. */
    @Transactional(readOnly = true)
    public ProvenanceResponse get(Long artworkId) {
        Artwork a = findOrThrow(artworkId);
        currentUser.currentId(); // exige sesión
        boolean privileged = isOwnerOrAdmin(a);
        if (a.getDueDiligenceStatus() == Artwork.DueDiligenceStatus.FLAGGED && !privileged) {
            throw ResourceNotFoundException.of("Obra", artworkId);
        }
        boolean seesDocuments = canViewDocuments(a);
        List<ProvenanceEntry> entries = entryRepository.findByArtworkIdOrderByFromYearAscIdAsc(artworkId);
        DueDiligenceCheck check = checkRepository.findFirstByArtworkIdOrderByCheckedAtDescIdDesc(artworkId).orElse(null);

        return ProvenanceResponse.builder()
                .artworkId(a.getId())
                .artworkTitle(a.getTitle())
                .artworkYear(a.getYear())
                .titleDeclarationText(Artwork.TITLE_DECLARATION)
                .titleDeclaredAt(a.getTitleDeclaredAt())
                .entries(entries.stream().map(e -> ProvenanceResponse.Entry.builder()
                        .id(e.getId())
                        .owner(e.getOwner())
                        .fromYear(e.getFromYear())
                        .toYear(e.getToYear())
                        .acquisitionMethod(e.getAcquisitionMethod().name())
                        .notes(e.getNotes())
                        .hasDocument(e.getDocument() != null)
                        .documentFileId(seesDocuments && e.getDocument() != null ? e.getDocument().getId() : null)
                        .documentFileName(seesDocuments && e.getDocument() != null ? e.getDocument().getFileName() : null)
                        .build()).toList())
                .gaps(findGaps(a, entries))
                .dueDiligenceStatus(status(a).name())
                .dueDiligence(check == null ? null : ProvenanceResponse.DueDiligence.builder()
                        .result(check.getResult().name())
                        .sources(check.getSources())
                        .reference(check.getReference())
                        .notes(currentUser.isAdmin() ? check.getNotes() : null)
                        .checkedByName(check.getCheckedBy().getName())
                        .checkedAt(check.getCheckedAt())
                        .build())
                .editable(privileged)
                .build();
    }

    // ===== Titularidad e historial (dueño o admin) =====

    @Transactional
    public ProvenanceResponse declareTitle(Long artworkId) {
        Artwork a = findOrThrow(artworkId);
        currentUser.requireUserId(a.getCollector().getId());
        if (a.getTitleDeclaredAt() == null) {
            a.setTitleDeclaredAt(LocalDateTime.now());
            artworkRepository.save(a);
        }
        return get(artworkId);
    }

    @Transactional
    public ProvenanceResponse addEntry(Long artworkId, ProvenanceEntryRequest req, MultipartFile file) throws IOException {
        Artwork a = findOrThrow(artworkId);
        requireOwnerOrAdmin(a);
        int thisYear = LocalDate.now().getYear();
        if (req.getFromYear() > thisYear || (req.getToYear() != null && req.getToYear() > thisYear)) {
            throw new IllegalArgumentException("Los años no pueden ser futuros.");
        }
        if (req.getToYear() != null && req.getToYear() < req.getFromYear()) {
            throw new IllegalArgumentException("El año final no puede ser anterior al inicial.");
        }
        if (a.getYear() != null && req.getFromYear() < a.getYear()) {
            throw new IllegalArgumentException("La obra es de " + a.getYear() + ": nadie pudo tenerla antes.");
        }

        DBFile doc = null;
        if (file != null && !file.isEmpty()) {
            String type = UploadValidator.requireType(file, UploadValidator.IMAGES_AND_PDF);
            doc = DBFile.builder()
                    .fileName(UploadValidator.safeFileName(file.getOriginalFilename(), "justificante"))
                    .fileType(type)
                    .data(file.getBytes())
                    .fileSize(file.getSize())
                    .build();
        }
        entryRepository.save(ProvenanceEntry.builder()
                .artwork(a)
                .owner(req.getOwner().trim())
                .fromYear(req.getFromYear())
                .toYear(req.getToYear())
                .acquisitionMethod(req.getAcquisitionMethod())
                .notes(req.getNotes() != null && !req.getNotes().isBlank() ? req.getNotes().trim() : null)
                .document(doc)
                .build());
        return get(artworkId);
    }

    @Transactional
    public ProvenanceResponse deleteEntry(Long artworkId, Long entryId) {
        Artwork a = findOrThrow(artworkId);
        requireOwnerOrAdmin(a);
        ProvenanceEntry e = entryRepository.findById(entryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Eslabón de procedencia", entryId));
        if (!e.getArtwork().getId().equals(artworkId)) {
            throw new AccessDeniedException("El eslabón no pertenece a esta obra.");
        }
        entryRepository.delete(e);
        return get(artworkId);
    }

    // ===== Diligencia debida (administración) =====

    @Transactional
    public ProvenanceResponse recordDueDiligence(Long artworkId, DueDiligenceRequest req) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo la administración registra la diligencia debida.");
        }
        if (req.getResult() == Artwork.DueDiligenceStatus.PENDING) {
            throw new IllegalArgumentException("El resultado debe ser CLEAR o FLAGGED.");
        }
        Artwork a = findOrThrow(artworkId);
        checkRepository.save(DueDiligenceCheck.builder()
                .artwork(a)
                .result(req.getResult())
                .sources(req.getSources().trim())
                .reference(req.getReference() != null && !req.getReference().isBlank() ? req.getReference().trim() : null)
                .notes(req.getNotes() != null && !req.getNotes().isBlank() ? req.getNotes().trim() : null)
                .checkedBy(userRepository.getReferenceById(currentUser.currentId()))
                .build());
        a.setDueDiligenceStatus(req.getResult());
        if (req.getResult() == Artwork.DueDiligenceStatus.FLAGGED) {
            // Se retira de la circulación hasta que se aclare.
            a.setAvailableForLoan(false);
        }
        artworkRepository.save(a);
        notifyOwner(a, req.getResult());
        return get(artworkId);
    }

    /** Obras pendientes de revisar, las que ya tienen titularidad declarada primero. */
    @Transactional(readOnly = true)
    public List<DueDiligencePendingItem> pending() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo la administración.");
        }
        return artworkRepository.findAll().stream()
                .filter(a -> status(a) == Artwork.DueDiligenceStatus.PENDING)
                .map(a -> {
                    List<ProvenanceResponse.Gap> gaps = findGaps(a,
                            entryRepository.findByArtworkIdOrderByFromYearAscIdAsc(a.getId()));
                    return DueDiligencePendingItem.builder()
                            .artworkId(a.getId())
                            .title(a.getTitle())
                            .artist(a.getArtist())
                            .year(a.getYear())
                            .collectorName(a.getCollector().getName())
                            .titleDeclaredAt(a.getTitleDeclaredAt())
                            .provenanceEntries(entryRepository.countByArtworkId(a.getId()))
                            .provenanceGaps(gaps.size())
                            .criticalGap(gaps.stream().anyMatch(ProvenanceResponse.Gap::isCritical))
                            .createdAt(a.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparing((DueDiligencePendingItem i) -> i.getTitleDeclaredAt() == null)
                        .thenComparing(DueDiligencePendingItem::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // ===== Reglas compartidas =====

    /**
     * Comprueba que la obra puede recibir solicitudes: titularidad
     * declarada y sin marca de sospecha. La usa {@link LoanRequestService}.
     */
    public void requireLendable(Artwork a) {
        if (status(a) == Artwork.DueDiligenceStatus.FLAGGED) {
            throw new IllegalStateException("Esta obra está retirada mientras se revisa su procedencia.");
        }
        if (a.getTitleDeclaredAt() == null) {
            throw new IllegalStateException(
                    "El coleccionista todavía no ha firmado la declaración de titularidad de esta obra.");
        }
    }

    /**
     * Los justificantes de procedencia los ven el dueño, la administración y
     * las fundaciones que han pedido la obra. También lo usa
     * {@link FileStorageService}.
     */
    public boolean canViewDocuments(Artwork a) {
        if (isOwnerOrAdmin(a)) return true;
        Long me = currentUser.currentIdOrNull();
        return me != null && loanRequestRepository.existsByArtworkIdAndFoundationId(a.getId(), me);
    }

    public static Artwork.DueDiligenceStatus status(Artwork a) {
        return a.getDueDiligenceStatus() != null ? a.getDueDiligenceStatus() : Artwork.DueDiligenceStatus.PENDING;
    }

    /**
     * Detecta los periodos sin propietario conocido: entre la creación de la
     * obra y el primer eslabón, entre eslabones y desde el último hasta hoy.
     */
    List<ProvenanceResponse.Gap> findGaps(Artwork a, List<ProvenanceEntry> entries) {
        List<ProvenanceResponse.Gap> gaps = new ArrayList<>();
        int thisYear = LocalDate.now().getYear();
        if (entries.isEmpty()) {
            if (a.getYear() != null) {
                gaps.add(gap(a.getYear(), thisYear, "No hay historial de procedencia."));
            }
            return gaps;
        }
        List<ProvenanceEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(ProvenanceEntry::getFromYear)).toList();

        int first = sorted.get(0).getFromYear();
        if (a.getYear() != null && first > a.getYear() + 1) {
            gaps.add(gap(a.getYear(), first, "Se desconoce quién tuvo la obra desde su creación hasta " + first + "."));
        }
        int coveredTo = end(sorted.get(0), thisYear);
        for (int i = 1; i < sorted.size(); i++) {
            ProvenanceEntry e = sorted.get(i);
            if (e.getFromYear() > coveredTo + 1) {
                gaps.add(gap(coveredTo, e.getFromYear(),
                        "Hueco entre " + coveredTo + " y " + e.getFromYear() + "."));
            }
            coveredTo = Math.max(coveredTo, end(e, thisYear));
        }
        if (coveredTo < thisYear - 1) {
            gaps.add(gap(coveredTo, thisYear, "No consta el propietario desde " + coveredTo + "."));
        }
        return gaps;
    }

    // ===== Helpers =====

    private static int end(ProvenanceEntry e, int thisYear) {
        return e.getToYear() != null ? e.getToYear() : thisYear;
    }

    private static ProvenanceResponse.Gap gap(int from, int to, String message) {
        boolean critical = from <= SPOLIATION_TO && to >= SPOLIATION_FROM;
        return ProvenanceResponse.Gap.builder()
                .fromYear(from)
                .toYear(to)
                .critical(critical)
                .message(critical
                        ? message + " Incluye " + SPOLIATION_FROM + "–" + SPOLIATION_TO
                                + ": los museos revisan este periodo con especial cuidado por el expolio nazi."
                        : message)
                .build();
    }

    private Artwork findOrThrow(Long id) {
        return artworkRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Obra", id));
    }

    private boolean isOwnerOrAdmin(Artwork a) {
        Long me = currentUser.currentIdOrNull();
        return currentUser.isAdminOrNull() || (me != null && me.equals(a.getCollector().getId()));
    }

    private void requireOwnerOrAdmin(Artwork a) {
        if (!isOwnerOrAdmin(a)) {
            throw new AccessDeniedException("Solo el dueño de la obra puede editar su procedencia.");
        }
    }

    private void notifyOwner(Artwork a, Artwork.DueDiligenceStatus result) {
        String email = a.getCollector().getEmail();
        if (email == null) return;
        String body = result == Artwork.DueDiligenceStatus.CLEAR
                ? "Hemos revisado la procedencia de «" + a.getTitle() + "» sin encontrar incidencias. "
                        + "La obra mostrará el distintivo de procedencia verificada."
                : "Al revisar la procedencia de «" + a.getTitle() + "» hemos encontrado indicios que debemos "
                        + "aclarar. La obra queda retirada del catálogo mientras tanto. Nos pondremos en contacto contigo.";
        emailService.sendSimpleEmail(email, "Revisión de procedencia: " + a.getTitle(),
                "Hola " + a.getCollector().getName() + ",\n\n" + body + "\n\nEl equipo de Prestarte.");
    }
}
