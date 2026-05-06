package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_company_id", nullable = false)
    private TransportCompany transportCompany;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShipmentStatus status;

    /**
     * Sentido del envío: ida (origen → museo) o vuelta (museo → coleccionista).
     * Cada préstamo genera un OUTBOUND y, al terminar, un RETURN.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private ShipmentDirection direction = ShipmentDirection.OUTBOUND;

    // --- Campos económicos y de seguro ---
    private Double price;
    private Double insuranceCost;
    private Double insuranceValue;
    private String insurancePolicy;

    @Builder.Default
    private boolean priceAccepted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String receivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime deliveryDate;

    public enum ShipmentStatus {
        REQUESTED,    // Asignado al transportista, sin presupuesto aún.
        QUOTED,       // Presupuesto subido, esperando aprobación del museo.
        REJECTED,     // El transportista no acepta el servicio.
        APPROVED,     // Presupuesto aprobado, esperando recogida.
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED
    }

    public enum ShipmentDirection {
        OUTBOUND,     // Origen (coleccionista) → destino (museo).
        RETURN        // Museo → coleccionista, al terminar el préstamo.
    }
}
