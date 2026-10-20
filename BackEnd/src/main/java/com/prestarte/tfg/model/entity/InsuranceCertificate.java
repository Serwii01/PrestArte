package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Certificado de seguro "clavo a clavo" de un préstamo.
 *
 * Cubre la obra desde que sale de casa del prestador hasta que vuelve.
 * Sin un certificado aprobado la obra no puede salir: ni el coleccionista
 * puede marcarla como lista para recoger ni el transportista recogerla.
 *
 * Un préstamo puede acumular varios certificados (si uno se rechaza, se
 * sube otro); el vigente es siempre el más reciente.
 */
@Entity
@Table(name = "insurance_certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoverageType coverageType;

    /** Aseguradora, o el ministerio en el caso de la Garantía del Estado. */
    @Column(nullable = false, length = 200)
    private String insurer;

    /** Número de póliza, o referencia de la orden ministerial / BOE. */
    @Column(nullable = false, length = 100)
    private String policyNumber;

    /** Suma asegurada para esta obra (€). */
    @Column(nullable = false)
    private Double insuredAmount;

    @Column(nullable = false)
    private LocalDate coverageStart;

    @Column(nullable = false)
    private LocalDate coverageEnd;

    /** Certificado en PDF. */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_file_id", nullable = false)
    private DBFile document;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    /** Motivo del rechazo, si el coleccionista no lo acepta. */
    @Column(length = 500)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    /** Quién asegura la obra. */
    public enum CoverageType {
        /** Póliza de la institución que recibe la obra (lo habitual). */
        BORROWER_POLICY,
        /** Póliza del propio prestador; la institución le reembolsa la prima. */
        LENDER_POLICY,
        /** Garantía del Estado (RD 1680/1991), solo en museos estatales. */
        STATE_INDEMNITY
    }

    /** Estado de revisión de un documento por la otra parte. */
    public enum ReviewStatus { PENDING, APPROVED, REJECTED }
}
