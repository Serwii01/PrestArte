package com.prestarte.tfg.repository;

import com.prestarte.tfg.model.entity.Collector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectorRepository extends JpaRepository<Collector, Long> {
}