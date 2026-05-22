package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solicitud de préstamo de una obra.
 *
 * Representa el expediente completo de un préstamo, desde que una
 * fundación lo solicita hasta que la obra regresa al coleccionista.
 * Su campo {@link #status} avanza a través de la máquina de estados
 * definida en {@code LoanStateMachine}; las transiciones se disparan
 * desde el servicio correspondiente y se sincronizan con el envío
 * asociado ({@link Shipment}).
 */
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

    /** Obra que se quiere prestar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    /** Fundación o museo que solicita el préstamo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foundation_id", nullable = false)
    private Foundation foundation;

    /** Fecha de inicio del préstamo. */
    private LocalDate startDate;

    /** Fecha de fin del préstamo. */
    private LocalDate endDate;

    /** Condiciones adicionales acordadas entre las partes, en texto libre. */
    @Column(columnDefinition = "TEXT")
    private String agreedConditions;

    /** Estado actual del expediente dentro de la máquina de estados. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Status status;

    /**
     * Indica si la empresa de transporte elegida por el coleccionista es
     * obligatoria. Cuando es true, un rechazo de presupuesto cancela el
     * préstamo en lugar de permitir reasignar a otra empresa.
     */
    @Builder.Default
    private boolean transportCompanyMandatory = false;

    /** Motivo de cancelación. Solo se rellena cuando el estado es CANCELLED. */
    @Column(length = 500)
    private String cancellationReason;

    /** Momento en que se canceló el préstamo. */
    private LocalDateTime cancelledAt;

    /** Estados posibles por los que pasa un préstamo a lo largo de su ciclo de vida. */
    public enum Status {
        /** Solicitud recién creada, pendiente de respuesta del coleccionista. */
        REQUESTED,
        /** El coleccionista rechaza la solicitud. Estado final. */
        REJECTED,
        /** El coleccionista acepta y selecciona empresa de transporte. */
        ACCEPTED,
        /** Esperando que la empresa de transporte suba su presupuesto. */
        QUOTE_PENDING,
        /** Presupuesto enviado, pendiente de aprobación por parte del museo. */
        QUOTE_PROPOSED,
        /** El museo aprueba y se considera pagado (paso simulado en esta versión). */
        PAID,
        /** El coleccionista confirma que la obra está lista para ser recogida. */
        READY_FOR_PICKUP,
        /** La obra está en tránsito hacia el museo. */
        IN_TRANSIT,
        /** El museo confirma la llegada de la obra. */
        DELIVERED,
        /** La obra se encuentra en exposición durante el periodo acordado. */
        ON_LOAN,
        /** Se ha iniciado la devolución y existe un envío de retorno activo. */
        RETURNING,
        /** La obra ha regresado al coleccionista. Estado final. */
        RETURNED,
        /** El préstamo se ha cancelado antes de completarse. Estado final. */
        CANCELLED
    }
}
