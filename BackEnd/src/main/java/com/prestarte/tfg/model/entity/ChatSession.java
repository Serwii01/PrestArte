package com.prestarte.tfg.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sesión de chat asociada a un préstamo.
 *
 * Cada préstamo dispone de una sesión de chat única que sirve como
 * canal de comunicación entre las tres partes (coleccionista, fundación
 * y empresa de transporte) y la administración. La sesión puede
 * cerrarse para impedir nuevos mensajes pero conserva el histórico.
 */
@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Préstamo al que pertenece esta sesión. La relación es uno a uno. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Estado de la sesión: abierta o cerrada para nuevos mensajes. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoChat estado = EstadoChat.ACTIVO;

    /** Fecha en la que se cerró la sesión, si procede. */
    private LocalDateTime closedAt;

    /** Mensajes que componen la conversación. */
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Message> messages;

    /** Estados posibles de la sesión de chat. */
    public enum EstadoChat {
        /** La sesión acepta nuevos mensajes. */
        ACTIVO,
        /** La sesión está cerrada; el histórico sigue siendo consultable. */
        CERRADO
    }
}
