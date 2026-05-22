package com.prestarte.tfg.model.entity;

/**
 * Cobertura geográfica que ofrece una empresa de transporte.
 *
 * Permite filtrar el listado de empresas en función del alcance del
 * envío que se quiere presupuestar.
 */
public enum CoverageArea {
    /** Servicio solo dentro del territorio nacional. */
    DOMESTIC,
    /** Servicio dentro de la Unión Europea o del Espacio Económico Europeo. */
    EUROPE,
    /** Servicio internacional sin restricciones geográficas. */
    INTERNATIONAL
}
