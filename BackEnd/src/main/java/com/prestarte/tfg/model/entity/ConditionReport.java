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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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

    /** Momento del envío en el que se levanta el informe. */
    public enum Stage {
        /** Recogida en origen, por la empresa de transporte. */
        PICKUP,
        /** Entrega en destino, por quien recibe la obra. */
        DELIVERY
    }
}
