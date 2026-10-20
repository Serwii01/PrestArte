package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Exposición para la que una fundación solicita obras.
 *
 * En la práctica un museo no pide una obra suelta: pide varias para un
 * proyecto concreto. Agrupar las solicitudes bajo una exposición da
 * contexto al coleccionista (qué se expone, dónde y cuándo) y permite a
 * la fundación seguir de un vistazo el estado de todas las obras.
 */
@Entity
@Table(name = "exhibitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exhibition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fundación organizadora. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foundation_id", nullable = false)
    private Foundation foundation;

    @Column(nullable = false, length = 200)
    private String title;

    /** Proyecto curatorial: qué cuenta la exposición y por qué se piden las obras. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Sala o sede donde se expone. */
    @Column(nullable = false, length = 200)
    private String venueName;

    /** Ciudad de la sede. */
    @Column(nullable = false, length = 150)
    private String venueCity;

    /**
     * País de la sede (código ISO de dos letras). Si no es España, las
     * obras antiguas o protegidas necesitan permiso de exportación.
     * Nulo equivale a "ES".
     */
    @Column(length = 2)
    private String venueCountry;

    /** Fechas de apertura y cierre al público. */
    @Column(nullable = false)
    private LocalDate openingDate;

    @Column(nullable = false)
    private LocalDate closingDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
