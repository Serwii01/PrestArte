package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Mensaje individual dentro de una {@link ChatSession}.
 *
 * Puede ser un mensaje de texto, una imagen, un documento adjunto o una
 * propuesta de acuerdo. El emisor siempre es uno de los usuarios con
 * acceso al chat (coleccionista, fundación, empresa de transporte o
 * administrador), y opcionalmente puede llevar un archivo adjunto.
 */
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

    /** Sesión a la que pertenece el mensaje. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    /** Usuario que envía el mensaje. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /** Contenido textual del mensaje. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    private LocalDateTime sentAt;

    /** Tipo de mensaje, que determina cómo se renderiza en la interfaz. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo = TipoMensaje.TEXTO;

    /** Archivo adjunto opcional, almacenado en {@link DBFile}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private DBFile attachment;

    /** Naturaleza del mensaje. */
    public enum TipoMensaje {
        /** Mensaje de solo texto. */
        TEXTO,
        /** Mensaje con una imagen adjunta. */
        IMAGEN,
        /** Mensaje con un documento adjunto. */
        DOCUMENTO,
        /** Propuesta o constancia formal de acuerdo. */
        ACUERDO
    }
}
