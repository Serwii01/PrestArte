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

    @OneToOne // Un envío pertenece a un único préstamo
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @ManyToOne
    @JoinColumn(name = "transport_company_id", nullable = false)
    private TransportCompany transportCompany;

    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String receivedBy;      // Quién firma en el museo

    @Column(columnDefinition = "TEXT") // Permite notas largas si es necesario
    private String notes;

    private LocalDateTime deliveryDate; // Se grabará cuando el museo confirme

    public enum ShipmentStatus {
        PENDIENTE, RECOGIDO, EN_TRANSITO, ENTREGADO
    }
}