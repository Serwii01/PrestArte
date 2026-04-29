package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.model.entity.User; // 1. Cambiamos el import para traer a User completo
import com.prestarte.tfg.repository.FoundationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoundationService {

    private final FoundationRepository foundationRepository;

    @Transactional
    public Foundation createFoundation(Foundation foundation) {
        // 2. Accedemos al Role a través de User
        foundation.setRole(User.Role.FOUNDATION);
        return foundationRepository.save(foundation);
    }

    public List<Foundation> getAllFoundations() {
        return foundationRepository.findAll();
    }
}