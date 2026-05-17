package com.prestarte.tfg.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body para PUT /api/transport-companies/{id}/profile.
 * El propietario de la empresa edita su ficha pública.
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
