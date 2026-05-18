package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByTransportCompanyId(Long transportCompanyId);

    List<Shipment> findByLoanRequestFoundationIdAndStatus(Long foundationId, Shipment.ShipmentStatus status);

    /** Shipment OUTBOUND del préstamo (legado: asume un único OUTBOUND). */
    Optional<Shipment> findByLoanRequestId(Long loanId);

    /** Lookup por dirección concreta (OUTBOUND o RETURN). Legado. */
    Optional<Shipment> findByLoanRequestIdAndDirection(Long loanId, Shipment.ShipmentDirection direction);

    /**
     * Último shipment de una dirección dada. Tras introducir la reasignación de
     * transportistas, un préstamo puede tener varios OUTBOUND (uno por intento):
     * el primero queda en REJECTED, los siguientes hasta que se apruebe uno.
     * Esta query devuelve el más reciente, que es el activo en el flujo.
     */
    Optional<Shipment> findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
            Long loanId, Shipment.ShipmentDirection direction);
}
