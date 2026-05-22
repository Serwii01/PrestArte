package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Subclase de {@link User} que representa a una empresa de transporte
 * especializada en bienes culturales.
 *
 * Las empresas de transporte presupuestan los servicios de envío asociados
 * a los préstamos, recogen las obras y las entregan a su destino. Disponen
 * además de un perfil público con descripción, especialidades y sedes que
 * cualquier visitante puede consultar en la sección de partners.
 */
@Entity
@Table(name = "transport_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TransportCompany extends User {

    /** Razón social o nombre comercial de la empresa. */
    @Column(length = 200, nullable = false)
    private String companyName;

    /** Ámbito geográfico que cubre la empresa (por ejemplo, "Nacional" o "Europa"). */
    @Column(length = 50)
    private String coverageArea;

    /** Correo de contacto comercial publicado en el perfil. */
    @Column(length = 150)
    private String contactEmail;

    /** URL del sitio web público de la empresa. */
    @Column(length = 200)
    private String website;

    /** Descripción visible en el perfil público. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Lista de especialidades como texto libre separado por comas.
     * Por ejemplo: "Pintura, Escultura, Gran formato".
     */
    @Column(length = 500)
    private String specialties;

    /**
     * Sedes u oficinas de la empresa, una por línea. El perfil público
     * las muestra como una lista.
     */
    @Column(columnDefinition = "TEXT")
    private String locations;
}
