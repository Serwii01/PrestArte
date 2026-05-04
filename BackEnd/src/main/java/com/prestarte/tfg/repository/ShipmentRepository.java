package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByTransportCompanyId(Long transportCompanyId);
}