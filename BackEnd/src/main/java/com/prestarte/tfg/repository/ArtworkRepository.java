package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    List<Artwork> findByOwnerId(Long ownerId); // <--- ESTE ES EL CLAVE
}