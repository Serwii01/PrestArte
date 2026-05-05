package com.prestarte.tfg.model.entity;

/**
 * Cobertura geográfica que ofrece una empresa de transporte.
 * Se usa como filtro al listar empresas para un préstamo concreto.
 */
public enum CoverageArea {
    DOMESTIC,        // Solo nacional
    EUROPE,          // Unión Europea / EEE
    INTERNATIONAL    // Global
}
