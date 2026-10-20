package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.LoanEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso al historial de los préstamos. */
@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {

    /** Historial de un préstamo en orden cronológico, con el autor de cada entrada. */
    @EntityGraph(attributePaths = "actor")
    List<LoanEvent> findByLoanRequestIdOrderByCreatedAtAscIdAsc(Long loanRequestId);
}
