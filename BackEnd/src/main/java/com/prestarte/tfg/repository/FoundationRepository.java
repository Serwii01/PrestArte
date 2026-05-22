package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Foundation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla de fundaciones y museos.
 *
 * No incluye consultas específicas: las búsquedas comunes sobre la
 * jerarquía de usuarios se realizan a través de {@code UserRepository}.
 */
public interface FoundationRepository extends JpaRepository<Foundation, Long> {
}
