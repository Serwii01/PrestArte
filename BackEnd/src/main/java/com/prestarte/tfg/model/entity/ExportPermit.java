package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Permiso de exportación temporal de una obra (Ley 16/1985 y RD 111/1986).
 *
 * Se exige cuando la obra sale de España y tiene cien o más años, o está
 * declarada Bien de Interés Cultural o incluida en el Inventario General.
 * Sin él la obra no puede salir; y si no vuelve antes de la fecha límite
 * que fija la resolución, la salida se considera exportación ilícita.
 */
@Entity
@Table(name = "export_permits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportPermit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    /** Número o referencia de la resolución. */
    @Column(nullable = false, length = 100)
    private String resolutionNumber;

    /** Órgano que la emite (Ministerio de Cultura, Junta de Calificación...). */
    @Column(nullable = false, length = 200)
    private String issuedBy;

    @Column(nullable = false)
    private LocalDate issueDate;

    /** Fecha límite en la que la obra debe haber vuelto a España. */
    @Column(nullable = false)
    private LocalDate returnDeadline;

    /** Resolución en PDF. */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_file_id", nullable = false)
    private DBFile document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
