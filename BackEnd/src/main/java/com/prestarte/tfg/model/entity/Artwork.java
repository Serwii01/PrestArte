package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Obra de arte publicada en el catálogo por un coleccionista.
 *
 * Recoge los datos descriptivos y físicos de la pieza (título, artista,
 * dimensiones, estado, valoración), las condiciones que el coleccionista
 * exige para prestarla y la lista de archivos asociados (fotografías y
 * documentación). Una obra puede señalar una empresa de transporte
 * preferida y marcarla como obligatoria; también puede deshabilitarse
 * temporalmente para que no se puedan iniciar nuevos préstamos sin
 * desaparecer del catálogo.
 */
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

    /** Título de la obra tal y como se mostrará en el catálogo. */
    @Column(length = 200, nullable = false)
    private String title;

    /** Nombre del artista o autor. */
    @Column(length = 200, nullable = false)
    private String artist;

    /** Año de creación, si se conoce. */
    @Column
    private Integer year;

    /** Anchura en centímetros. */
    @Column
    private Double widthCm;

    /** Altura en centímetros. */
    @Column
    private Double heightCm;

    /** Profundidad en centímetros; solo es relevante en piezas tridimensionales. */
    @Column(nullable = true)
    private Double depthCm;

    /** Estado de conservación declarado por el coleccionista. */
    @Enumerated(EnumType.STRING)
    @Column(name = "artwork_condition", nullable = false)
    private Condition condition;

    /** Descripción libre con técnica, soporte y contexto histórico. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Valor estimado en euros, utilizado como base del seguro. */
    @Column(nullable = false)
    private Double estimatedValue;

    /** Condiciones que el museo debe cumplir durante la exposición (clima, luz, manipulación...). */
    @Column(columnDefinition = "TEXT")
    private String loanConditions;

    /** Ciudad o localidad donde se encuentra físicamente la obra. */
    @Column(length = 150)
    private String location;

    /** Coleccionista propietario de la obra. Toda obra tiene siempre un dueño. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private Collector collector;

    /**
     * Empresa de transporte que el coleccionista propone preferentemente
     * para esta obra. Si {@link #preferredTransportMandatory} es true, el
     * museo solo podrá negociar con esta empresa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_transport_company_id")
    private TransportCompany preferredTransportCompany;

    /** Indica si la empresa preferida es obligatoria o solo una recomendación. */
    @Builder.Default
    @Column(nullable = false)
    private boolean preferredTransportMandatory = false;

    /**
     * Si vale false, la obra se sigue mostrando en el catálogo con un aviso
     * de "deshabilitada" pero las fundaciones no pueden crear nuevas
     * solicitudes de préstamo. El coleccionista puede volver a habilitarla
     * en cualquier momento.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean availableForLoan = true;

    /** Fotografías y documentos asociados a la obra. */
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL)
    private List<ArtworkFile> files;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Estado de conservación posible de una obra. */
    public enum Condition {
        EXCELLENT, GOOD, FAIR, POOR, DAMAGED
    }
}
