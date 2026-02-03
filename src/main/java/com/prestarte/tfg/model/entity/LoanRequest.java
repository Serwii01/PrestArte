package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    private LocalDateTime requestedDate;

    @OneToOne(mappedBy = "loanRequest", cascade = CascadeType.ALL)
    private ChatSession chatSession;

    @Column(columnDefinition = "TEXT")
    private String agreedConditions;  // JSON condiciones finales

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum EstadoSolicitud {
        PENDIENTE, APROBADO, RECHAZADO, CANCELADO, EN_CURSO
    }
}
