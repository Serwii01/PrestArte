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

    /**
     * Requisitos estructurados de préstamo, comparables con el informe de
     * instalaciones de la fundación. Puede ser nulo si no hay ninguno.
     */
    @Embedded
    private LoanRequirements requirements;

    // ===== Condiciones del prestador =====

    /**
     * Texto de la cartela y créditos, tal y como quiere aparecer el
     * prestador (p. ej. "Colección particular, Madrid").
     */
    @Column(length = 200)
    private String creditLine;

    /** Usos autorizados de la imagen de la obra durante el préstamo. */
    @Embedded
    private ImageRights imageRights;

    /** Canon que pide el prestador por el préstamo (€). Nulo o 0 = gratuito. */
    private Double loanFee;

    /**
     * Ciudad o provincia donde se encuentra la obra. Es el único dato de
     * ubicación que se muestra públicamente.
     */
    @Column(length = 150)
    private String location;

    /**
     * Dirección exacta de recogida. Es privada: solo la ven el dueño, la
     * administración y la empresa de transporte con un envío aprobado y
     * en curso para esta obra.
     */
    @Column(length = 500)
    private String pickupAddress;

    /**
     * Si es false (por defecto), la obra aparece como "Colección
     * particular" y el nombre del coleccionista no se muestra a terceros
     * hasta que acepta un préstamo.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean showOwnerName = false;

    /**
     * Protección patrimonial de la obra. Junto con su antigüedad decide
     * si necesita permiso de exportación para salir de España. Nulo
     * equivale a NONE.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HeritageStatus heritageStatus;

    // ===== Titularidad y procedencia =====

    /**
     * Momento en que el coleccionista firmó la declaración de titularidad
     * ({@link #TITLE_DECLARATION}). Sin ella la obra no recibe solicitudes.
     */
    private LocalDateTime titleDeclaredAt;

    /** Resultado vigente de la diligencia debida. Nulo equivale a PENDING. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DueDiligenceStatus dueDiligenceStatus;

    /** Quién puede ver la obra en el catálogo. Nulo equivale a PUBLIC. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Visibility visibility = Visibility.PUBLIC;

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

    /** Texto de la declaración de titularidad que firma el coleccionista. */
    public static final String TITLE_DECLARATION =
            "Declaro que soy el legítimo propietario de esta obra o que estoy autorizado por él para "
            + "prestarla; que, según mi conocimiento, no ha sido robada, expoliada ni exportada "
            + "ilícitamente; y que no conozco reclamaciones de terceros sobre ella. Me comprometo a "
            + "comunicar a PrestArte cualquier reclamación que pueda surgir.";

    /** Resultado de la diligencia debida de la administración. */
    public enum DueDiligenceStatus {
        /** Sin revisar todavía. */
        PENDING,
        /** Revisada sin incidencias. */
        CLEAR,
        /** Hay indicios de problema: la obra se retira del catálogo. */
        FLAGGED
    }

    /** Protección patrimonial según la Ley 16/1985. */
    public enum HeritageStatus {
        /** Sin protección específica. */
        NONE,
        /** Incluida en el Inventario General de Bienes Muebles. */
        INVENTORIED,
        /** Declarada Bien de Interés Cultural. */
        BIC
    }

    /** Visibilidad de la obra en el catálogo. */
    public enum Visibility {
        /** Cualquier visitante, con o sin cuenta. */
        PUBLIC,
        /** Solo usuarios verificados de la red (con sesión iniciada). */
        MEMBERS
    }
}
