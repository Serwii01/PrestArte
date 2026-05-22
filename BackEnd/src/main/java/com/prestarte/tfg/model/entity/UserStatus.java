package com.prestarte.tfg.model.entity;

/**
 * Estado de revisión de una cuenta de usuario.
 *
 * Toda cuenta nueva entra en {@link #PENDING} hasta que un administrador
 * la revisa. La transición a {@link #APPROVED} habilita el acceso al
 * sistema; {@link #REJECTED} deniega definitivamente el alta.
 */
public enum UserStatus {
    /** Cuenta recién registrada, a la espera de revisión. */
    PENDING,
    /** Cuenta aprobada por el administrador y habilitada para iniciar sesión. */
    APPROVED,
    /** Cuenta rechazada en la revisión inicial. */
    REJECTED
}
