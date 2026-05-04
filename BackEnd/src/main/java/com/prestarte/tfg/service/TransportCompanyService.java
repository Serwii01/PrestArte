package com.prestarte.tfg.service;

import com.prestarte.tfg.model.entity.TransportCompany;
import com.prestarte.tfg.repository.TransportCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransportCompanyService {

    private final TransportCompanyRepository transportCompanyRepository;

    @Transactional
    public TransportCompany registerCompany(TransportCompany company) {
        // Aquí podrías añadir lógica para cifrar contraseña si tuvieras Security
        return transportCompanyRepository.save(company);
    }
}