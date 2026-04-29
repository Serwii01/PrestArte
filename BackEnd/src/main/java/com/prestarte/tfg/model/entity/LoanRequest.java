package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "loan_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @ManyToOne
    @JoinColumn(name = "foundation_id", nullable = false)
    private Foundation foundation;

    // Cambiamos los nombres para que el servicio los encuentre
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;

    @Column(columnDefinition = "TEXT")
    private String agreedConditions;

    @Enumerated(EnumType.STRING)
    @Column(length = 20) // Añade esto para asegurar espacio
    private Status status;

    public enum Status {
        PENDIENTE, ACEPTADA, RECHAZADA, FINALIZADA
    }
}