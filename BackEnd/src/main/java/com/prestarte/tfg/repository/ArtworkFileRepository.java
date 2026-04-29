package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ArtworkFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtworkFileRepository extends JpaRepository<ArtworkFile, Long> {
}