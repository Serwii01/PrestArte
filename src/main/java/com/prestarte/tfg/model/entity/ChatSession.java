package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

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

    @OneToOne
    @JoinColumn(name = "loan_request_id", nullable = false)
    private LoanRequest loanRequest;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoChat estado;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL)
    private List<Message> messages;

    public enum EstadoChat {
        ACTIVO, CERRADO
    }
}
