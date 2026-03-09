package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportCompany extends User {

    @Column(length = 20)
    private String taxId;           // CIF empresa

    @Column(length = 200, nullable = false)
    private String companyName;     // "Transportes ArteSeguro S.L."

    @Column(length = 50)
    private String coverageArea;    // "Nacional", "Europa", "Internacional"

    @Column(length = 150)
    private String contactEmail;    // contacto@transporte.com
}
