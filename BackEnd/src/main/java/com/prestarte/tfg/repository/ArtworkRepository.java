package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    // Para que un coleccionista vea solo su inventario
    List<Artwork> findByCollectorId(Long collectorId);

    // Para buscar obras por artista o título en el buscador de la fundación
    List<Artwork> findByArtistContainingIgnoreCaseOrTitleContainingIgnoreCase(String artist, String title);
}