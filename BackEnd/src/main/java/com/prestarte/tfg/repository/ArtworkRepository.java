package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Acceso a la tabla de obras.
 *
 * Las operaciones CRUD básicas las proporciona {@link JpaRepository}; el
 * único método específico permite recuperar todas las obras de un
 * coleccionista concreto.
 */
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    /** Devuelve las obras cuyo coleccionista propietario tiene el id indicado. */
    List<Artwork> findByCollectorId(Long collectorId);
}
