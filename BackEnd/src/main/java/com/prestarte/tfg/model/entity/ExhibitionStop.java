package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Sede siguiente de una exposición itinerante. La sede principal es la
 * de la propia {@link Exhibition}; estas son las paradas posteriores de la
 * gira, en orden. La obra viaja de una sede a otra sin volver al prestador.
 */
@Entity
@Table(name = "exhibition_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExhibitionStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    /** Orden en la gira: 1 es la primera sede después de la principal. */
    @Column(nullable = false)
    private Integer position;

    /** Institución que acoge la exposición en esta sede. */
    @Column(nullable = false, length = 200)
    private String hostInstitution;

    @Column(nullable = false, length = 200)
    private String venueName;

    @Column(nullable = false, length = 150)
    private String venueCity;

    /** ISO de dos letras; nulo equivale a "ES". */
    @Column(length = 2)
    private String venueCountry;

    @Column(nullable = false)
    private LocalDate openingDate;

    @Column(nullable = false)
    private LocalDate closingDate;
}
