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

    @OneToOne
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @ManyToOne
    @JoinColumn(name = "transport_company_id", nullable = false)
    private TransportCompany transportCompany;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    // --- Nuevos campos para Negociación y Seguro ---
    private Double price;                // Coste del transporte
    private Double insuranceCost;        // Coste de la prima del seguro
    private Double insuranceValue;       // Valor total asegurado (valor de la obra)
    private String insurancePolicy;      // Número de póliza

    @Builder.Default
    private boolean priceAccepted = false; // ¿Ha aceptado la Fundación el presupuesto?

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String receivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime deliveryDate;

    public enum ShipmentStatus {
        SOLICITADO,    // La empresa aún no ha aceptado o enviado presupuesto
        RECHAZADO,     // La empresa no puede realizar el servicio
        PENDIENTE,     // Presupuesto aceptado, esperando recogida
        RECOGIDO,
        EN_TRANSITO,
        ENTREGADO
    }
}