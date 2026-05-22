package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla de envíos.
 *
 * Un préstamo puede tener varios envíos a lo largo de su ciclo: el de
 * ida y el de vuelta, y opcionalmente varios envíos OUTBOUND si el
 * presupuesto se rechaza y se reasigna a otra empresa. Por eso, además
 * de los buscadores convencionales, se expone una consulta que devuelve
 * el último envío de una dirección concreta, que es el que se considera
 * activo en cada momento.
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /** Devuelve los envíos asignados a una empresa de transporte concreta. */
    List<Shipment> findByTransportCompanyId(Long transportCompanyId);

    /** Devuelve los envíos en un estado concreto de los préstamos de una fundación. */
    List<Shipment> findByLoanRequestFoundationIdAndStatus(Long foundationId, Shipment.ShipmentStatus status);

    /** Devuelve cualquier envío asociado al préstamo indicado. */
    Optional<Shipment> findByLoanRequestId(Long loanId);

    /** Devuelve el envío de una dirección concreta dentro del préstamo. */
    Optional<Shipment> findByLoanRequestIdAndDirection(Long loanId, Shipment.ShipmentDirection direction);

    /**
     * Devuelve el envío más reciente de la dirección indicada para un
     * préstamo. Se utiliza para obtener el envío activo cuando hay
     * varios OUTBOUND como consecuencia de una reasignación de empresa
     * de transporte.
     */
    Optional<Shipment> findFirstByLoanRequestIdAndDirectionOrderByCreatedAtDesc(
            Long loanId, Shipment.ShipmentDirection direction);
}
