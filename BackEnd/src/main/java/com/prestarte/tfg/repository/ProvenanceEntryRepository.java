package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.ProvenanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso al historial de procedencia de las obras. */
@Repository
public interface ProvenanceEntryRepository extends JpaRepository<ProvenanceEntry, Long> {

    /** Historial de una obra en orden cronológico. */
    List<ProvenanceEntry> findByArtworkIdOrderByFromYearAscIdAsc(Long artworkId);

    long countByArtworkId(Long artworkId);

    Optional<ProvenanceEntry> findByDocumentId(String fileId);
}
