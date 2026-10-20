package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Exhibition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a las exposiciones de las fundaciones. */
@Repository
public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {

    /** Exposiciones de una fundación, las más próximas primero. */
    List<Exhibition> findByFoundationIdOrderByOpeningDateAsc(Long foundationId);
}
