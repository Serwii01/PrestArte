package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.DBFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla de archivos binarios.
 *
 * El identificador es de tipo {@code String} porque la entidad
 * {@link DBFile} usa un UUID generado por Hibernate como clave primaria.
 */
@Repository
public interface DBFileRepository extends JpaRepository<DBFile, String> {
}
