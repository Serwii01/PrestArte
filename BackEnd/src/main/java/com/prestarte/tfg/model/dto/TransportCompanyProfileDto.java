package com.prestarte.tfg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Perfil público de una empresa de transporte. No expone datos sensibles
 * (email del usuario, taxId, status interno...). Lo consume el catálogo
 * público de partners y el detalle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportCompanyProfileDto {
    private Long id;
    private String companyName;
    private String contactEmail;
    private String website;
    private String description;
    private String specialties;
    private String locations;
    private String coverageArea;
}
