package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.DueDiligenceCheck;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a las comprobaciones de diligencia debida. */
@Repository
public interface DueDiligenceCheckRepository extends JpaRepository<DueDiligenceCheck, Long> {

    /** Última comprobación de una obra. */
    @EntityGraph(attributePaths = "checkedBy")
    Optional<DueDiligenceCheck> findFirstByArtworkIdOrderByCheckedAtDescIdDesc(Long artworkId);
}
