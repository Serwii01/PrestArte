package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Informe de estado de la obra ("condition report") en un hito del envío.
 *
 * Se levanta obligatoriamente en dos momentos de cada envío:
 * <ul>
 *   <li>{@link Stage#PICKUP}: la empresa de transporte documenta cómo
 *       recoge la obra.</li>
 *   <li>{@link Stage#DELIVERY}: quien la recibe (el museo en la ida, el
 *       coleccionista en la vuelta) documenta cómo llega.</li>
 * </ul>
 * Con las fotografías de ambos extremos se puede demostrar en qué tramo
 * apareció un daño.
 */
@Entity
@Table(name = "condition_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Envío en el que se levanta el informe. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    // varchar y no ENUM de MySQL: así se pueden añadir fases sin migrar la columna.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private Stage stage;

    /** Estado de conservación observado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "artwork_condition", nullable = false, length = 20)
    private Artwork.Condition condition;

    /** Indica que quien firma el informe ha observado daños o incidencias. */
    @Builder.Default
    @Column(nullable = false)
    private boolean damageReported = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Fotografías de la obra en el momento del informe. */
    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "condition_report_photos",
            joinColumns = @JoinColumn(name = "condition_report_id"),
            inverseJoinColumns = @JoinColumn(name = "file_id"))
    private List<DBFile> photos = new ArrayList<>();

    /** Usuario que firma el informe. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Daños señalados sobre las fotos. */
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DamageMark> marks = new ArrayList<>();

    // ===== Firma de conformidad de la otra parte =====

    /** Quien firma la conformidad (la otra parte del momento: ver ConditionReportService). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by_id")
    private User acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    /** True si la otra parte firma que NO está conforme con el informe. */
    private Boolean disputed;

    @Column(length = 500)
    private String acknowledgementComment;

    /** Momento en el que se levanta el informe. */
    public enum Stage {
        /** Recogida en origen, por la empresa de transporte. */
        PICKUP,
        /** Entrega en destino, por quien recibe la obra. */
        DELIVERY,
        /** En sala, antes de desmontar la exposición, por la institución. */
        PRE_RETURN,
        /** En sala, antes del traslado a la siguiente sede de una gira, por la institución. */
        TRANSFER
    }
}
