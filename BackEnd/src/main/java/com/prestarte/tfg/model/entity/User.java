package com.prestarte.tfg.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Entidad base de cualquier persona o entidad con cuenta en Prestarte.
 *
 * Es abstracta y se hereda mediante estrategia JOINED por las cuatro
 * subclases concretas: {@link Admin}, {@link Collector}, {@link Foundation}
 * y {@link TransportCompany}. Concentra los datos comunes a todos los
 * roles (credenciales, contacto, identificación fiscal y documento de
 * verificación) y deja en las subclases los campos específicos de cada
 * tipo de usuario.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico único. Sirve también como nombre de usuario para iniciar sesión. */
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    /** Nombre legible del usuario (persona física o razón social). */
    @Column(nullable = false, length = 100)
    private String name;

    /** Contraseña cifrada con BCrypt. Nunca se serializa en las respuestas. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    /** Teléfono de contacto. Validado en el registro como número español. */
    @Column(length = 20)
    private String phone;

    /** Rol funcional dentro de la plataforma: ADMIN, COLLECTOR, FOUNDATION o TRANSPORT. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ===== Campos de verificación (KYB) =====

    /** Estado de la cuenta. Las cuentas nuevas nacen pendientes de aprobación. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    /** Identificador fiscal del usuario: DNI, NIE o CIF según el caso. */
    @Column(length = 50)
    private String taxId;

    /**
     * Documento aportado en el registro (DNI, escritura, etc.) que el
     * administrador revisa antes de aprobar la cuenta.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_file_id")
    private DBFile verificationFile;

    // ========================================

    /** Indica si la cuenta puede iniciar sesión. Solo se habilita tras la aprobación. */
    @Builder.Default
    private boolean enabled = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
