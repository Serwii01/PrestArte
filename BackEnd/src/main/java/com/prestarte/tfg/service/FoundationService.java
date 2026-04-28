package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.Foundation;
import com.prestarte.tfg.repository.FoundationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoundationService {

    private final FoundationRepository foundationRepository;

    public Foundation createFoundation(Foundation foundation) {
        return foundationRepository.save(foundation);
    }

    public List<Foundation> getAllFoundations() {
        return foundationRepository.findAll();
    }
}