package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.model.entity.Role;
import com.prestarte.tfg.repository.FoundationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoundationService {

    private final FoundationRepository foundationRepository;

    @Transactional
    public Foundation createFoundation(Foundation foundation) {
        // Asignamos el rol usando el enum
        foundation.setRole(Role.FOUNDATION);
        return foundationRepository.save(foundation);
    }

    public List<Foundation> getAllFoundations() {
        return foundationRepository.findAll();
    }
}