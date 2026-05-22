package com.prestarte.tfg.model.entity;

/**
 * Rol funcional de un usuario dentro de la plataforma.
 *
 * Cada uno de los cuatro valores se corresponde con una subclase concreta
 * de {@link User} y determina qué acciones puede realizar el usuario y
 * qué pantallas tiene disponibles en la aplicación.
 */
public enum Role {
    /** Coleccionista particular dueño de obras de arte. */
    COLLECTOR,
    /** Museo o fundación que solicita préstamos para exposiciones. */
    FOUNDATION,
    /** Empresa especializada en el transporte de bienes culturales. */
    TRANSPORT,
    /** Administrador de la plataforma encargado de aprobar nuevas altas. */
    ADMIN
}
