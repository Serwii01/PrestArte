package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ArtworkFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla que vincula obras con sus archivos.
 *
 * No requiere consultas específicas más allá de las que ofrece
 * {@link JpaRepository} para crear, eliminar y buscar por identificador.
 */
@Repository
public interface ArtworkFileRepository extends JpaRepository<ArtworkFile, Long> {
}
