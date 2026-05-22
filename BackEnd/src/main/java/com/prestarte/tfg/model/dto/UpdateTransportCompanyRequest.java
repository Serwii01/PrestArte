package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload utilizado para actualizar el perfil público de una empresa
 * de transporte.
 *
 * Todos los campos son opcionales: la acción solo modifica los
 * valores que vengan informados, lo que permite cambios parciales
 * desde la propia cuenta de la empresa o desde la administración.
 */
@Data
public class UpdateTransportCompanyRequest {

    @Size(max = 200)
    private String companyName;

    @Size(max = 150)
    private String contactEmail;

    @Size(max = 200)
    private String website;

    private String description;

    @Size(max = 500)
    private String specialties;

    private String locations;

    @Size(max = 50)
    private String coverageArea;
}
