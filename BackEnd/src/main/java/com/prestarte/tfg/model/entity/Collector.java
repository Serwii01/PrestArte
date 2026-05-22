package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Subclase de {@link User} que representa a un coleccionista particular.
 *
 * Un coleccionista es el propietario de una o varias obras y la parte que
 * acepta o rechaza las solicitudes de préstamo. Añade los datos postales
 * que se incluyen en el contrato cuando se formaliza un préstamo.
 */
@Entity
@Table(name = "collectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Collector extends User {

    /** Dirección postal donde se localizan las obras. */
    @Column(length = 255)
    private String address;

    /** Ciudad o localidad del coleccionista. */
    @Column(length = 100)
    private String city;

    /** Código postal asociado a la dirección. */
    @Column(length = 10)
    private String postalCode;
}
