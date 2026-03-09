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

    @Column(length = 20)
    private String taxId;           // CIF empresa

    @Column(length = 200, nullable = false)
    private String companyName;

    @Column(length = 50)
    private String coverageArea;    // "Nacional", "Europa", etc.

    @Column(length = 150)
    private String contactEmail;
}