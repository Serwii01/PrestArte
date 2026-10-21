package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Eslabón del historial de procedencia de una obra: quién la tuvo, desde
 * cuándo, hasta cuándo y cómo la adquirió.
 *
 * Un historial completo y documentado es lo que da confianza a un museo
 * para pedir la obra (y lo que exige si quiere inmunidad frente a embargo
 * en préstamos internacionales). Los huecos, sobre todo entre 1933 y 1945,
 * son una señal de alerta.
 */
@Entity
@Table(name = "provenance_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvenanceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    /** Propietario o poseedor (puede ser genérico: "Colección particular, Madrid"). */
    @Column(nullable = false, length = 300)
    private String owner;

    /** Año desde el que la tuvo. */
    @Column(nullable = false)
    private Integer fromYear;

    /** Año hasta el que la tuvo; nulo si es el propietario actual. */
    private Integer toYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcquisitionMethod acquisitionMethod;

    /** Detalles: casa de subastas y lote, galería, notario, referencias... */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Justificante opcional: factura, catálogo de subasta, testamento, certificado... */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "document_file_id")
    private DBFile document;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Cómo pasó la obra a este propietario. */
    public enum AcquisitionMethod {
        /** El propio artista (primer eslabón). */
        ARTIST,
        PURCHASE,
        AUCTION,
        INHERITANCE,
        GIFT,
        COMMISSION,
        EXCHANGE,
        UNKNOWN
    }
}
