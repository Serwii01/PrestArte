package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ArtworkFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a la tabla que vincula obras con sus archivos.
 *
 * Además de las operaciones de {@link JpaRepository}, permite localizar
 * la obra a la que pertenece un archivo para decidir quién puede verlo.
 */
@Repository
public interface ArtworkFileRepository extends JpaRepository<ArtworkFile, Long> {

    /** Busca el vínculo obra-archivo a partir del UUID del {@code DBFile}. */
    Optional<ArtworkFile> findByFileId(String fileId);
}
