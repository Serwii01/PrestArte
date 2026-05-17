package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "transport_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TransportCompany extends User {

    // taxId se hereda de User (campo común a todos los usuarios).

    @Column(length = 200, nullable = false)
    private String companyName;

    @Column(length = 50)
    private String coverageArea;    // "Nacional", "Europa", etc.

    @Column(length = 150)
    private String contactEmail;

    /** URL del sitio web público de la empresa. */
    @Column(length = 200)
    private String website;

    /** Descripción visible en el perfil público. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Especialidades (texto libre separado por comas).
     * Ej. "Pintura, Escultura, Gran formato, Antigüedades".
     */
    @Column(length = 500)
    private String specialties;

    /**
     * Sedes / oficinas. Texto multi-línea libre.
     * Ej. "Madrid (HQ)\nBarcelona\nLisboa".
     */
    @Column(columnDefinition = "TEXT")
    private String locations;
}