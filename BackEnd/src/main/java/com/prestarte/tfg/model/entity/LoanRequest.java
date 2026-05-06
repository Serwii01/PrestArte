package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foundation_id", nullable = false)
    private Foundation foundation;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String agreedConditions;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Status status;

    /**
     * Si es true, el coleccionista exige específicamente la empresa de transporte
     * elegida. Si el museo rechaza el presupuesto de esa empresa, el préstamo
     * se cancela. Si es false, el museo puede pedir presupuesto a otra empresa.
     */
    @Builder.Default
    private boolean transportCompanyMandatory = false;

    /** Razón de cancelación. Solo se rellena cuando status = CANCELLED. */
    @Column(length = 500)
    private String cancellationReason;

    /** Momento de cancelación. Audit trail. */
    private LocalDateTime cancelledAt;

    public enum Status {
        REQUESTED,           // Museo solicita, esperando al coleccionista.
        REJECTED,            // Coleccionista rechaza. Estado final.
        ACCEPTED,            // Coleccionista acepta + elige empresa de transporte.
        QUOTE_PENDING,       // Esperando presupuesto del transportista.
        QUOTE_PROPOSED,      // Transportista propone presupuesto, esperando al museo.
        PAID,                // Museo aprueba presupuesto y paga (simulado).
        READY_FOR_PICKUP,    // Coleccionista confirma que la obra está lista para recoger.
        IN_TRANSIT,          // El transportista ha recogido y está en ruta.
        DELIVERED,           // El museo confirma la llegada.
        ON_LOAN,             // Periodo activo de exposición.
        RETURNING,           // Retorno iniciado: nuevo Shipment de vuelta.
        RETURNED,            // Obra devuelta al coleccionista. Estado final.
        CANCELLED            // Cancelado en cualquier punto. Estado final.
    }
}
