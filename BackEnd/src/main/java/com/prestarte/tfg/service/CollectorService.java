package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.repository.CollectorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final CollectorRepository collectorRepository;

    @Transactional
    public Collector createCollector(Collector collector) {
        // Asignamos el rol usando el enum
        collector.setRole(Role.COLLECTOR);
        return collectorRepository.save(collector);
    }

    public List<Collector> getAllCollectors() {
        return collectorRepository.findAll();
    }
}