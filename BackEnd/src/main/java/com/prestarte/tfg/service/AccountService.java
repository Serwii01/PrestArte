package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.*;
import com.prestarte.tfg.repository.*;
import com.prestarte.tfg.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Derechos del usuario sobre sus datos (RGPD): acceso/portabilidad
 * (exportación en JSON) y supresión (baja de la cuenta).
 *
 * La baja anonimiza los datos personales en lugar de borrar la fila: los
 * préstamos, contratos e historiales en los que participó el usuario
 * deben conservarse por obligaciones legales y porque también son datos
 * de las otras partes. El documento de verificación se elimina.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Estados en los que un préstamo sigue vivo y bloquea la baja. */
    private static final Set<LoanRequest.Status> CLOSED = EnumSet.of(
            LoanRequest.Status.RETURNED, LoanRequest.Status.REJECTED, LoanRequest.Status.CANCELLED);

    private final UserRepository userRepository;
    private final ArtworkRepository artworkRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final DBFileRepository dbFileRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    // ===== Derecho de acceso y portabilidad =====

    /** Todos los datos del usuario con sesión, en una estructura apta para JSON. */
    @Transactional(readOnly = true)
    public Map<String, Object> exportMyData() {
        User u = userRepository.findById(currentUser.currentId()).orElseThrow();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", LocalDateTime.now());
        out.put("termsVersion", u.getTermsVersion());

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", u.getId());
        profile.put("name", u.getName());
        profile.put("email", u.getEmail());
        profile.put("phone", u.getPhone());
        profile.put("taxId", u.getTaxId());
        profile.put("role", u.getRole());
        profile.put("status", u.getStatus());
        profile.put("createdAt", u.getCreatedAt());
        profile.put("termsAcceptedAt", u.getTermsAcceptedAt());
        if (u instanceof Collector c) {
            profile.put("address", c.getAddress());
            profile.put("city", c.getCity());
            profile.put("postalCode", c.getPostalCode());
        } else if (u instanceof Foundation f) {
            profile.put("institutionName", f.getInstitutionName());
            profile.put("address", f.getAddress());
            profile.put("city", f.getCity());
        } else if (u instanceof TransportCompany t) {
            profile.put("companyName", t.getCompanyName());
            profile.put("contactEmail", t.getContactEmail());
            profile.put("website", t.getWebsite());
        }
        out.put("profile", profile);

        out.put("artworks", artworkRepository.findByCollectorId(u.getId()).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("artist", a.getArtist());
            m.put("year", a.getYear());
            m.put("location", a.getLocation());
            m.put("pickupAddress", a.getPickupAddress());
            m.put("estimatedValue", a.getEstimatedValue());
            m.put("createdAt", a.getCreatedAt());
            return m;
        }).toList());

        out.put("loans", myLoans(u).stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("artwork", l.getArtwork().getTitle());
            m.put("status", l.getStatus());
            m.put("startDate", l.getStartDate());
            m.put("endDate", l.getEndDate());
            m.put("exhibition", l.getExhibition() != null ? l.getExhibition().getTitle() : null);
            return m;
        }).toList());

        out.put("notifications", notificationRepository.findByRecipientIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(n -> Map.of("title", n.getTitle(), "message", n.getMessage(), "createdAt", n.getCreatedAt()))
                .toList());

        out.put("reviewsWritten", reviewRepository.findByReviewerIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(r -> reviewMap(r)).toList());
        out.put("reviewsReceived", reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(r -> reviewMap(r)).toList());
        return out;
    }

    // ===== Derecho de supresión =====

    /**
     * Da de baja la cuenta del usuario con sesión, previa confirmación de
     * su contraseña. No se permite con préstamos en curso.
     */
    @Transactional
    public void deleteMyAccount(String password) {
        User u = userRepository.findById(currentUser.currentId()).orElseThrow();
        if (u.getRole() == Role.ADMIN) {
            throw new AccessDeniedException("Las cuentas de administración no se dan de baja desde aquí.");
        }
        if (password == null || !passwordEncoder.matches(password, u.getPassword())) {
            throw new IllegalArgumentException("La contraseña no es correcta.");
        }
        boolean active = myLoans(u).stream().anyMatch(l -> !CLOSED.contains(l.getStatus()));
        if (active) {
            throw new IllegalStateException(
                    "Tienes préstamos en curso. Termínalos o cancélalos antes de darte de baja.");
        }

        // Las obras dejan de estar disponibles (y salen del catálogo).
        for (Artwork a : artworkRepository.findByCollectorId(u.getId())) {
            a.setAvailableForLoan(false);
            a.setPickupAddress(null);
            artworkRepository.save(a);
        }

        DBFile verification = u.getVerificationFile();
        u.setVerificationFile(null);

        u.setName("Usuario eliminado");
        u.setEmail("eliminado-" + u.getId() + "@prestarte.invalid");
        u.setPhone(null);
        u.setTaxId(null);
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setEnabled(false);
        u.setDeletedAt(LocalDateTime.now());
        if (u instanceof Collector c) {
            c.setAddress(null);
            c.setPostalCode(null);
        } else if (u instanceof Foundation f) {
            f.setAddress(null);
        } else if (u instanceof TransportCompany t) {
            t.setCompanyName("Empresa eliminada");
            t.setContactEmail(null);
            t.setWebsite(null);
        }
        userRepository.save(u);
        if (verification != null) {
            dbFileRepository.delete(verification);
        }
    }

    // ===== Helpers =====

    /** Préstamos en los que participa el usuario, sea cual sea su rol. */
    private List<LoanRequest> myLoans(User u) {
        Map<Long, LoanRequest> loans = new LinkedHashMap<>();
        loanRequestRepository.findByArtworkCollectorId(u.getId()).forEach(l -> loans.put(l.getId(), l));
        loanRequestRepository.findByFoundationId(u.getId()).forEach(l -> loans.put(l.getId(), l));
        shipmentRepository.findByTransportCompanyId(u.getId())
                .forEach(s -> loans.put(s.getLoanRequest().getId(), s.getLoanRequest()));
        return new ArrayList<>(loans.values());
    }

    private static Map<String, Object> reviewMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loanId", r.getLoanRequest().getId());
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}
