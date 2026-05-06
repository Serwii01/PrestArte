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
    private String coverageArea;    // "Nacional", "Europa", etc. → en sub-fase 2.5 pasa a enum.

    @Column(length = 150)
    private String contactEmail;
}