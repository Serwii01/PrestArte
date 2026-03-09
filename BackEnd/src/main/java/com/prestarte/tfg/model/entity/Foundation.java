package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "foundations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Foundation extends User {

    @Column(length = 20)
    private String taxId;        // CIF fundación

    @Column(length = 200, nullable = false)
    private String institutionName;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;
}