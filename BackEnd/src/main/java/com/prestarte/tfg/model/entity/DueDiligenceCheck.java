package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Comprobación de diligencia debida que hace la administración sobre una
 * obra: si consta en bases de datos de obras robadas o expoliadas y si su
 * procedencia es razonable.
 *
 * Se guarda cada comprobación (no se sobrescribe) para dejar rastro de
 * quién revisó qué y cuándo. El resultado vigente se copia en
 * {@link Artwork#getDueDiligenceStatus()}.
 */
@Entity
@Table(name = "due_diligence_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DueDiligenceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Artwork.DueDiligenceStatus result;

    /** Bases consultadas: Interpol ID-Art, Art Loss Register, Lost Art... */
    @Column(nullable = false, length = 300)
    private String sources;

    /** Referencia del certificado o de la búsqueda, si la hay. */
    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_id", nullable = false)
    private User checkedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime checkedAt;
}
