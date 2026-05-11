package com.prestarte.tfg.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para reseteo de contraseña.
 * Se genera cuando un usuario solicita "olvidé mi contraseña" y se invalida tras
 * usarse o al expirar.
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

    /** UUID que viaja por email en la URL. */
    @Column(unique = true, nullable = false, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Lo invalidamos tras usarlo o si caduca. */
    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
