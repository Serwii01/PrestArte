package com.prestarte.tfg.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO público con el perfil de una empresa de transporte.
 *
 * Lo consumen tanto la sección pública de partners como los
 * selectores internos. Solo contiene la información que la empresa
 * desea publicar, sin exponer datos administrativos como su estado
 * o su identificador fiscal.
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
