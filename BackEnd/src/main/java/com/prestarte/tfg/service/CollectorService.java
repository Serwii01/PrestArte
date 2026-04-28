package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Collector;
import com.prestarte.tfg.repository.CollectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final CollectorRepository collectorRepository;

    public Collector createCollector(Collector collector) {
        return collectorRepository.save(collector);
    }

    public List<Collector> getAllCollectors() {
        return collectorRepository.findAll();
    }
}