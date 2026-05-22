package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.TransportCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla de empresas de transporte.
 *
 * Expone la comprobación de unicidad del identificador fiscal que se
 * utiliza puntualmente en algunos flujos administrativos.
 */
@Repository
public interface TransportCompanyRepository extends JpaRepository<TransportCompany, Long> {

    /** Indica si ya existe una empresa de transporte con ese CIF. */
    boolean existsByTaxId(String taxId);
}
