package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Marca sobre una foto de un informe de estado: señala dónde está un daño
 * o una incidencia (posición relativa, en %, para que no dependa del
 * tamaño con que se muestre la imagen) y lo describe.
 */
@Entity
@Table(name = "damage_marks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Excluido de equals/hashCode/toString para no entrar en bucle con el informe.
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_report_id", nullable = false)
    private ConditionReport report;

    /** Foto marcada. */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private DBFile photo;

    /** Posición horizontal y vertical sobre la foto, de 0 a 100. */
    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Column(length = 500)
    private String note;
}
