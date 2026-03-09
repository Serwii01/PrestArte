package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "collectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collector extends User {

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;
}
