package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para el flujo de "olvidé mi contraseña".
 *
 * Se crea cuando un usuario solicita recuperar su contraseña y viaja
 * dentro del enlace que se envía por correo electrónico. El token deja
 * de ser válido en cuanto se utiliza o cuando llega su fecha de
 * caducidad, lo que limita la ventana de uso indebido.
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador aleatorio (UUID) que viaja en la URL de recuperación. */
    @Column(unique = true, nullable = false, length = 100)
    private String token;

    /** Usuario al que pertenece el token. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Fecha y hora a partir de la cual el token deja de ser válido. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Indica si el token ya se ha utilizado para restablecer la contraseña. */
    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Indica si el token todavía puede emplearse: aún no se ha consumido
     * y no ha caducado.
     */
    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
