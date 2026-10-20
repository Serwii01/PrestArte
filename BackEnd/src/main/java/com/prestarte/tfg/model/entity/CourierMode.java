package com.prestarte.tfg.model.entity;

/**
 * Cómo supervisa el prestador el embalaje, el transporte y el montaje.
 *
 * El correo presencial (alguien del prestador que viaja con la obra) es
 * caro y el viaje del correo puede contaminar más que el propio
 * transporte; por eso el sector empuja hacia el correo virtual.
 */
public enum CourierMode {
    /** Sin supervisión del prestador. */
    NONE,
    /**
     * Supervisión remota: el embalaje se documenta con fotos en PrestArte
     * y el coleccionista lo aprueba antes de que la obra salga.
     */
    VIRTUAL,
    /** Un correo del prestador acompaña a la obra (gastos a cargo de la institución). */
    IN_PERSON
}
