package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.SupervisionCheckpoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a los hitos de supervisión remota de los préstamos. */
@Repository
public interface SupervisionCheckpointRepository extends JpaRepository<SupervisionCheckpoint, Long> {

    @EntityGraph(attributePaths = {"photos", "author"})
    List<SupervisionCheckpoint> findByLoanRequestIdOrderByCreatedAtAscIdAsc(Long loanRequestId);

    boolean existsByLoanRequestIdAndStageAndStatus(Long loanRequestId, SupervisionCheckpoint.Stage stage,
                                                   SupervisionCheckpoint.Status status);

    /** Busca el hito al que pertenece una foto, para comprobar quién puede verla. */
    @Query("SELECT c FROM SupervisionCheckpoint c JOIN c.photos p WHERE p.id = :fileId")
    Optional<SupervisionCheckpoint> findByPhotoId(@Param("fileId") String fileId);
}
