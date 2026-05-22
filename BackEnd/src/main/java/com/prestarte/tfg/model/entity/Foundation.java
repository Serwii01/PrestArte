package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Subclase de {@link User} que representa a un museo o fundación.
 *
 * Las fundaciones son las entidades que solicitan préstamos de obras a los
 * coleccionistas para exponerlas. Añaden el nombre institucional y los
 * datos postales del centro receptor, necesarios para el contrato y el
 * envío.
 */
@Entity
@Table(name = "foundations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Foundation extends User {

    /** Nombre oficial del museo o fundación (puede diferir del nombre del usuario). */
    @Column(length = 200, nullable = false)
    private String institutionName;

    /** Dirección postal del centro donde se expondrá la obra. */
    @Column(length = 255)
    private String address;

    /** Ciudad o localidad de la institución. */
    @Column(length = 100)
    private String city;
}
