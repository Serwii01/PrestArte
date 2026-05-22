package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Envío logístico asociado a un préstamo.
 *
 * Cada préstamo cuenta con un envío de ida (OUTBOUND) y, al iniciar la
 * devolución, otro de vuelta (RETURN). Por eso la relación con
 * {@link LoanRequest} es {@code ManyToOne}: distintos envíos pueden
 * compartir el mismo préstamo distinguiéndose por su {@link #direction}.
 * Además, si un presupuesto se rechaza y se reasigna a otra empresa, se
 * crea un nuevo Shipment OUTBOUND y el anterior queda con estado
 * REJECTED como parte del histórico.
 */
@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Préstamo al que pertenece este envío. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    /** Empresa de transporte responsable del envío. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_company_id", nullable = false)
    private TransportCompany transportCompany;

    /** Código de seguimiento legible para mostrarlo a las partes implicadas. */
    private String trackingNumber;

    /** Estado del envío dentro de su máquina de estados. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShipmentStatus status;

    /** Sentido del envío: ida hacia el museo o vuelta al coleccionista. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private ShipmentDirection direction = ShipmentDirection.OUTBOUND;

    // ===== Datos económicos y de seguro =====

    /** Precio total del servicio de transporte. */
    private Double price;

    /** Coste de la póliza de seguro contratada. */
    private Double insuranceCost;

    /** Valor asegurado de la obra durante el transporte. */
    private Double insuranceValue;

    /** Número de póliza o referencia del seguro. */
    private String insurancePolicy;

    /** Indica si el museo ha aceptado el precio propuesto. */
    @Builder.Default
    private boolean priceAccepted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Nombre de la persona que firma la recepción de la obra. */
    @Column(length = 100)
    private String receivedBy;

    /** Notas u observaciones recogidas en el momento de la entrega. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Fecha y hora en que se confirmó la entrega. */
    private LocalDateTime deliveryDate;

    /** Estados posibles de un envío. */
    public enum ShipmentStatus {
        /** Envío creado, a la espera de que la empresa suba un presupuesto. */
        REQUESTED,
        /** Presupuesto enviado, pendiente de aprobación por parte del museo. */
        QUOTED,
        /** La empresa de transporte declina el servicio o el museo rechaza el presupuesto. */
        REJECTED,
        /** Presupuesto aprobado; el envío está listo para iniciarse. */
        APPROVED,
        /** La obra ha sido recogida en origen. */
        PICKED_UP,
        /** La obra está en tránsito hacia su destino. */
        IN_TRANSIT,
        /** La obra ha llegado y se ha confirmado la entrega. */
        DELIVERED
    }

    /** Sentido logístico del envío. */
    public enum ShipmentDirection {
        /** Trayecto de ida: del coleccionista al museo. */
        OUTBOUND,
        /** Trayecto de vuelta: del museo al coleccionista al finalizar el préstamo. */
        RETURN
    }
}
