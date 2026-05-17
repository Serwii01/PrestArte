package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "artworks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 200, nullable = false)
    private String artist;

    @Column
    private Integer year;

    @Column
    private Double widthCm;

    @Column
    private Double heightCm;

    @Column(nullable = true)
    private Double depthCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "artwork_condition", nullable = false)
    private Condition condition;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double estimatedValue;

    @Column(columnDefinition = "TEXT")
    private String loanConditions;

    /** Ciudad / pueblo donde se encuentra físicamente la obra. */
    @Column(length = 150)
    private String location;

    // UNIFICADO: Usamos collector como única referencia al dueño.
    // Marcamos nullable = false porque toda obra debe tener un dueño en el sistema.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private Collector collector;

    /**
     * Empresa de transporte que el coleccionista prefiere para esta obra.
     * Se aplica al aceptar un préstamo: si está marcada como obligatoria,
     * el museo no podrá pedir presupuesto a otra empresa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_transport_company_id")
    private TransportCompany preferredTransportCompany;

    @Builder.Default
    @Column(nullable = false)
    private boolean preferredTransportMandatory = false;

    /**
     * Si es false, la obra se sigue mostrando en el catálogo pero no se pueden
     * crear nuevas solicitudes de préstamo sobre ella. Útil cuando el
     * coleccionista la retira temporalmente sin querer borrarla.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean availableForLoan = true;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL)
    private List<ArtworkFile> files;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Condition {
        EXCELLENT, GOOD, FAIR, POOR, DAMAGED
    }
}