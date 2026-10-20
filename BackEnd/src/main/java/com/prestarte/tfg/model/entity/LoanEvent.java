package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entrada del historial de un préstamo.
 *
 * Cada cambio de estado y cada hito relevante (reasignación de
 * transporte, informes de estado de la obra, rechazos automáticos...)
 * queda registrado con quién lo hizo y cuándo. Es la base de la
 * trazabilidad del préstamo: el historial no se modifica ni se borra.
 */
@Entity
@Table(name = "loan_events", indexes = @Index(name = "idx_loan_events_loan", columnList = "loan_request_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Préstamo al que pertenece la entrada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    /** Estado anterior, si la entrada es un cambio de estado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LoanRequest.Status fromStatus;

    /** Estado nuevo, si la entrada es un cambio de estado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LoanRequest.Status toStatus;

    /** Descripción legible del hito. */
    @Column(nullable = false, length = 1000)
    private String message;

    /**
     * Usuario que provocó el hito. Es nulo cuando lo genera el sistema
     * (por ejemplo, un rechazo automático por solapamiento de fechas).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
