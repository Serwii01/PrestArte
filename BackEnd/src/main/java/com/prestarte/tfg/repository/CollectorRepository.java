package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Collector;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de coleccionistas.
 *
 * Hereda las operaciones estándar de {@link JpaRepository}; no necesita
 * consultas adicionales porque las búsquedas sobre la jerarquía de
 * usuarios se realizan a través de {@code UserRepository}.
 */
public interface CollectorRepository extends JpaRepository<Collector, Long> {
}
