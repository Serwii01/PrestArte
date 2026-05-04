package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.TransportCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransportCompanyRepository extends JpaRepository<TransportCompany, Long> {
}