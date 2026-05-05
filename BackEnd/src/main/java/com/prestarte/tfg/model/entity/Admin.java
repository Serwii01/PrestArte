package com.prestarte.tfg.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Subclase de User para administradores de la plataforma.
 * No añade campos propios: hereda email, password, role, etc. de User.
 * Existe como entidad propia para mantener la coherencia con la jerarquía
 * @Inheritance(JOINED) y permitir distinguir administradores en consultas.
 */
@Entity
@Table(name = "admins")
@NoArgsConstructor
@SuperBuilder
public class Admin extends User {
}
