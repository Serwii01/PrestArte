package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;  // texto o "" si es imagen

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo;

    private boolean isClosed = false;  // "Cerrar trato"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private DBFile attachment;  // foto/PDF opcional

    public enum TipoMensaje {
        TEXTO, IMAGEN, DOCUMENTO, ACUERDO
    }
}
